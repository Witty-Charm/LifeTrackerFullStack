package com.lifetracker.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.lifetracker.mobile.BuildConfig
import com.lifetracker.mobile.core.sync.SyncScheduler
import com.lifetracker.mobile.domain.model.CreateTaskParams
import com.lifetracker.mobile.domain.model.UpdateTaskParams
import com.lifetracker.mobile.domain.model.DomainResult
import com.lifetracker.mobile.domain.model.GameError
import com.lifetracker.mobile.domain.model.GameTaskDomain
import com.lifetracker.mobile.domain.model.HabitPolarity
import com.lifetracker.mobile.domain.model.HabitResetPeriod
import com.lifetracker.mobile.domain.model.HeroDomain
import com.lifetracker.mobile.domain.model.HeroSnapshot
import com.lifetracker.mobile.domain.model.TaskType
import com.lifetracker.mobile.domain.model.dataOrNull
import com.lifetracker.mobile.domain.model.errorOrNull
import com.lifetracker.mobile.domain.model.fold
import com.lifetracker.mobile.domain.model.onFailure
import com.lifetracker.mobile.domain.model.onSuccess
import com.lifetracker.mobile.domain.usecase.hero.HeroUseCases
import com.lifetracker.mobile.domain.usecase.task.TaskUseCases
import com.lifetracker.mobile.ui.mapper.toDomain
import com.lifetracker.mobile.ui.mapper.toUi
import com.lifetracker.mobile.ui.mapper.toUiError
import com.lifetracker.mobile.ui.model.HeroScreenState
import com.lifetracker.mobile.ui.model.TaskActionFeedback
import com.lifetracker.mobile.ui.model.TaskPendingAction
import com.lifetracker.mobile.ui.model.TaskUi
import com.lifetracker.mobile.ui.model.UiDifficulty
import com.lifetracker.mobile.ui.model.UiEvent
import com.lifetracker.mobile.ui.model.UiTaskType
import com.lifetracker.mobile.ui.model.UiTaskType as TaskUiType
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class HeroViewModel(
    private val heroUseCases: HeroUseCases,
    private val taskUseCases: TaskUseCases,
    private val workManager: WorkManager,
) : ViewModel() {
    private val isDebug: Boolean = BuildConfig.DEBUG

    private companion object {
        const val FOREGROUND_REFRESH_DEBOUNCE_MS = 30_000L
        const val SILENT_REFRESH_DEBOUNCE_MS = 350L
        const val TASK_ACTION_FAILED_MESSAGE = "Action failed. Please try again."
    }

    private object ActionKeys {
        const val HERO_CREATE = "hero_create"
        const val HERO_RESPAWN = "hero_respawn"
        const val HERO_HEAL = "hero_heal"
        const val TASK_CREATE = "task_create"

        fun taskComplete(id: Int) = "task_complete_$id"

        fun taskFail(id: Int) = "task_fail_$id"

        fun taskDelete(id: Int) = "task_delete_$id"

        fun taskUpdate(id: Int) = "task_update_$id"
    }

    private val _state = MutableStateFlow(HeroScreenState())
    val state: StateFlow<HeroScreenState> = _state.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var loadJob: Job? = null
    private var refreshTasksJob: Job? = null
    private var lastForegroundRefreshAt: Long = 0L

    // confined to Main thread via viewModelScope - do not read/write from IO/Default context
    private var heroDomain: HeroDomain? = null
        set(value) {
            field = value
            _state.update { it.copy(hero = value?.toUi()) }
        }
    private val heroId: Int? get() = heroDomain?.id

    init {
        loadData()
        observeSyncWorker()
    }

    fun loadData() {
        loadJob?.cancel()
        loadJob =
            viewModelScope.launch {
                _state.update {
                    it.copy(
                        isLoading = true,
                        criticalError = null,
                        actionError = null,
                        needsHeroCreation = false,
                    )
                }

                val hero =
                    fetchHero() ?: run {
                        _state.update { it.copy(isLoading = false) }
                        return@launch
                    }

                try {
                    heroDomain = hero

                    val overdueDeferred = async { safeCall { taskUseCases.checkOverdue(hero.id) } }
                    val tasksDeferred = async { safeCall { taskUseCases.getTasks(hero.id) } }

                    val overdue = overdueDeferred.await()
                    val tasks = tasksDeferred.await()

                    _state.update { current ->
                        current.copy(
                            hero = hero.toUi(),
                            tasks = tasks.dataOrNull()?.toVisibleUiTasks() ?: current.tasks,
                            actionError = tasks.errorOrNull()?.toUiError(),
                        )
                    }
                    overdue.dataOrNull()?.let {
                        if (it.overdueCount > 0) {
                            _events.send(UiEvent.ShowSnackbar(it.message))
                        }
                    }
                    overdue.errorOrNull()?.let {
                        Timber.w("checkOverdueTasks failed: $it")
                    }
                } finally {
                    _state.update { it.copy(isLoading = false) }
                }
            }
    }

    fun refreshOnForeground(nowMillis: Long = System.currentTimeMillis()) {
        if (loadJob?.isActive == true) return
        if (lastForegroundRefreshAt != 0L && nowMillis - lastForegroundRefreshAt < FOREGROUND_REFRESH_DEBOUNCE_MS) return
        lastForegroundRefreshAt = nowMillis
        loadData()
    }

    fun completeTask(taskId: Int) =
        launchAction(ActionKeys.taskComplete(taskId)) {
            val task = findTask(taskId) ?: return@launchAction
            if (isServerMutationBlocked(task)) {
                showOfflineTaskActionBlockedMessage()
                return@launchAction
            }
            val originalIsCheckedToday = task.isCheckedToday
            patchTask(taskId) {
                it.copy(
                    pendingAction = TaskPendingAction.Complete,
                    actionError = null,
                    isCheckedToday = if (it.type == TaskUiType.Daily) !it.isCheckedToday else it.isCheckedToday,
                )
            }
            val result =
                if (task.type == TaskUiType.Daily) {
                    val heroTimeZoneId = heroDomain?.timeZoneId ?: TimeZone.currentSystemDefault().id
                    val today = Clock.System.now().toLocalDateTime(TimeZone.of(heroTimeZoneId)).date.toString()
                    executeAction { taskUseCases.setDailyTaskState(taskId, today, !originalIsCheckedToday) }
                } else {
                    executeAction { taskUseCases.completeTask(taskId) }
                }

            result?.let { actionResult ->
                applySnapshot(actionResult.heroSnapshot)
                when (task.type) {
                    TaskUiType.OneTime -> removeTask(taskId)
                    TaskUiType.Daily,
                    TaskUiType.Habit,
                    TaskUiType.Unknown,
                    -> patchTask(taskId) { current -> current.copy(pendingAction = null, actionError = null) }
                }
                requestTasksRefresh()
                _events.send(
                    UiEvent.TaskAction(
                        TaskActionFeedback.Completed(
                            xpGained = actionResult.xpGained,
                            goldGained = actionResult.goldGained,
                            leveledUp = actionResult.leveledUp,
                            newLevel = actionResult.newLevel.takeIf { actionResult.leveledUp },
                        ),
                    ),
                )
                for (achievement in actionResult.unlockedAchievements) {
                    _events.send(UiEvent.ShowSnackbar("Achievement unlocked: ${achievement.title} (+${achievement.goldReward} Gold)"))
                }
            } ?: patchTask(taskId) { current ->
                current.copy(
                    pendingAction = null,
                    actionError = TASK_ACTION_FAILED_MESSAGE,
                    isCheckedToday = if (current.type == TaskUiType.Daily) originalIsCheckedToday else current.isCheckedToday,
                )
            }
        }

    fun failTask(taskId: Int) =
        launchAction(ActionKeys.taskFail(taskId)) {
            val task = findTask(taskId) ?: return@launchAction
            if (isServerMutationBlocked(task)) {
                showOfflineTaskActionBlockedMessage()
                return@launchAction
            }
            patchTask(taskId) { it.copy(pendingAction = TaskPendingAction.Fail, actionError = null) }
            executeAction { taskUseCases.failTask(taskId) }
                ?.let { result ->
                    applySnapshot(result.heroSnapshot)
                    patchTask(taskId) { current -> current.copy(pendingAction = null, actionError = null) }
                    requestTasksRefresh()
                    _events.send(
                        UiEvent.TaskAction(
                            TaskActionFeedback.Failed(
                                hpLost = result.damageDealt,
                                goldLost = result.goldLost,
                                shieldAbsorbed = result.shieldAbsorbed && !result.streakBroken,
                            ),
                        ),
                    )
                } ?: patchTask(taskId) { current -> current.copy(pendingAction = null, actionError = TASK_ACTION_FAILED_MESSAGE) }
        }

    fun createTask(
        title: String,
        description: String?,
        type: UiTaskType,
        difficulty: UiDifficulty,
        dueDate: kotlin.time.Instant?,
        habitPolarity: HabitPolarity = HabitPolarity.Both,
        habitResetPeriod: HabitResetPeriod = HabitResetPeriod.Default,
    ) = launchAction(ActionKeys.TASK_CREATE) {
        val id = heroId ?: return@launchAction
        val params =
            CreateTaskParams(
                heroId = id,
                title = title,
                description = description,
                type = type.toDomain(),
                difficulty = difficulty.toDomain(),
                habitPolarity = habitPolarity,
                habitResetPeriod = habitResetPeriod,
                dueDate = dueDate,
            )
        executeAction { taskUseCases.createTask(params) }
            ?.let { task ->
                _state.update { current ->
                    current.copy(
                        tasks = (current.tasks + task.toUi()).toPersistentList(),
                    )
                }
                _events.send(UiEvent.TaskCreated(type))
            }
    }

    fun updateTask(
        taskId: Int,
        title: String,
        description: String?,
        type: UiTaskType,
        difficulty: UiDifficulty,
        dueDate: kotlin.time.Instant?,
        habitPolarity: HabitPolarity = HabitPolarity.Both,
        habitResetPeriod: HabitResetPeriod = HabitResetPeriod.Default,
        onSuccess: (() -> Unit)? = null,
    ) = launchAction(ActionKeys.taskUpdate(taskId)) {
        val params =
            UpdateTaskParams(
                taskId = taskId,
                type = type.toDomain(),
                title = title,
                description = description,
                difficulty = difficulty.toDomain(),
                habitPolarity = habitPolarity,
                habitResetPeriod = habitResetPeriod,
                dueDate = dueDate,
            )
        executeAction { taskUseCases.updateTask(params) }
            ?.let { task ->
                val updated = task.toUi()
                _state.update { current ->
                    current.copy(
                        tasks =
                            current.tasks
                                .map { if (it.id == taskId) updated else it }
                                .toPersistentList(),
                    )
                }
                _events.send(UiEvent.TaskUpdated(type))
                onSuccess?.invoke()
            }
    }

    fun deleteTask(taskId: Int) {
        val task = findTask(taskId) ?: return
        if (task.pendingAction != null) return
        if (taskId in _state.value.pendingDeletionTaskIds) return
        if (taskId < 0) {
            launchAction(ActionKeys.taskDelete(taskId)) {
                executeAction { taskUseCases.deleteTask(taskId) }?.let { removeTask(taskId) }
            }
            return
        }
        _state.update { state ->
            state.copy(pendingDeletionTaskIds = (state.pendingDeletionTaskIds + taskId).toPersistentSet())
        }
        viewModelScope.launch {
            _events.send(
                UiEvent.UndoDeletePrompt(
                    taskId = taskId,
                    message = deleteUndoMessage(task.type),
                    taskType = task.type,
                ),
            )
        }
    }

    fun undoDeleteTask(taskId: Int) {
        if (taskId !in _state.value.pendingDeletionTaskIds) return
        _state.update { state ->
            state.copy(pendingDeletionTaskIds = (state.pendingDeletionTaskIds - taskId).toPersistentSet())
        }
    }

    fun confirmDeleteTask(taskId: Int) {
        if (taskId !in _state.value.pendingDeletionTaskIds) return
        launchAction(ActionKeys.taskDelete(taskId)) {
            executeAction { taskUseCases.deleteTask(taskId) }
                ?.let {
                    _state.update { state ->
                        state.copy(
                            tasks = state.tasks.filter { it.id != taskId }.toPersistentList(),
                            pendingDeletionTaskIds = (state.pendingDeletionTaskIds - taskId).toPersistentSet(),
                        )
                    }
                    requestTasksRefresh()
                } ?: run {
                    _state.update { state ->
                        state.copy(pendingDeletionTaskIds = (state.pendingDeletionTaskIds - taskId).toPersistentSet())
                    }
                    patchTask(taskId) { current -> current.copy(actionError = TASK_ACTION_FAILED_MESSAGE) }
                }
        }
    }

    private fun deleteUndoMessage(type: UiTaskType): String =
        when (type) {
            UiTaskType.Habit -> "Habit deleted"
            UiTaskType.Daily -> "Daily deleted"
            UiTaskType.OneTime -> "To-Do deleted"
            UiTaskType.Unknown -> "Task deleted"
        }

    fun retrySync(taskId: Int) =
        launchAction("retry_sync_$taskId") {
            executeAction { taskUseCases.retryTaskSync(taskId) }
        }

    fun deleteFailedTask(taskId: Int) =
        launchAction("delete_failed_$taskId") {
            executeAction { taskUseCases.deleteLocalTask(taskId) }
                ?.let {
                    _state.update { current ->
                        current.copy(tasks = current.tasks.filter { it.id != taskId }.toPersistentList())
                    }
                }
        }

    fun respawnHero() =
        launchAction(ActionKeys.HERO_RESPAWN) {
            val id = heroId ?: return@launchAction
            executeAction { heroUseCases.respawnHero(id) }
                ?.let { result ->
                    updateHero {
                        copy(
                            currentHp = result.newHp,
                            maxHp = result.maxHp,
                            isDead = false,
                            deathCount = result.deathCount,
                            isInRecovery = result.recoveryDebuffActive,
                            recoveryMultiplier = result.recoveryMultiplier,
                        )
                    }
                    _events.send(
                        UiEvent.HeroRespawned(
                            message = result.message,
                            recoveryEndsAt = result.recoveryEndsAt,
                        ),
                    )
                }
        }

    fun healHero(amount: Int? = null) =
        launchAction(ActionKeys.HERO_HEAL) {
            val id = heroId ?: return@launchAction
            executeAction { heroUseCases.healHero(id, amount) }
                ?.let { result ->
                    updateHero {
                        copy(
                            currentHp = result.newHp,
                            maxHp = result.maxHp,
                            gold = result.newGold,
                        )
                    }
                    _events.send(UiEvent.HeroHealed(result.message))
                }
        }

    fun updateHeroGold(newGold: Int) {
        updateHero { copy(gold = newGold) }
    }

    fun updateHeroHp(
        newHp: Int,
        maxHp: Int,
    ) {
        updateHero { copy(currentHp = newHp, maxHp = maxHp) }
    }

    fun updateHeroXpBoost(
        percent: Int,
        tasksRemaining: Int,
    ) {
        updateHero { copy(xpBoostPercent = percent, xpBoostTasksRemaining = tasksRemaining) }
    }

    fun updateHeroRecovery(
        isInRecovery: Boolean,
        recoveryMultiplier: Double,
    ) {
        updateHero { copy(isInRecovery = isInRecovery, recoveryMultiplier = recoveryMultiplier) }
    }

    fun clearError() {
        _state.update { it.copy(criticalError = null, actionError = null) }
    }

    private fun updateHero(transform: HeroDomain.() -> HeroDomain) {
        heroDomain = heroDomain?.transform()
    }

    fun createHero(
        name: String,
        startingGold: Int? = null,
    ) = launchAction(ActionKeys.HERO_CREATE) {
        executeAction { heroUseCases.createHero(name, startingGold) }
            ?.let { hero ->
                heroDomain = hero
                _state.update { it.copy(hero = hero.toUi(), needsHeroCreation = false) }
                doRefreshTasks()
            }
    }

    private fun findTask(taskId: Int): TaskUi? =
        _state.value.tasks.firstOrNull { it.id == taskId }

    suspend fun loadTaskForEdit(taskId: Int): GameTaskDomain? =
        safeCall { taskUseCases.getTask(taskId) }.dataOrNull()

    private fun isServerMutationBlocked(task: TaskUi): Boolean =
        task.pendingAction != null || task.id < 0 || task.isPendingSync || task.syncError != null

    private suspend fun showOfflineTaskActionBlockedMessage() {
        _events.send(UiEvent.ShowSnackbar("Task is not synced yet. Try again when online."))
    }

    private fun patchTask(
        taskId: Int,
        transform: (TaskUi) -> TaskUi,
    ) {
        _state.update { state ->
            state.copy(
                tasks = state.tasks.map { task -> if (task.id == taskId) transform(task) else task }.toPersistentList(),
            )
        }
    }

    private fun removeTask(taskId: Int) {
        _state.update { state ->
            state.copy(tasks = state.tasks.filter { it.id != taskId }.toPersistentList())
        }
    }

    private fun requestTasksRefresh() {
        if (refreshTasksJob?.isActive == true) return
        refreshTasksJob =
            viewModelScope.launch {
                delay(SILENT_REFRESH_DEBOUNCE_MS)
                doRefreshTasks()
            }
    }

    private fun launchAction(
        key: String,
        block: suspend () -> Unit,
    ) {
        if (key in _state.value.loadingActions) return
        viewModelScope.launch {
            _state.update { it.copy(loadingActions = (it.loadingActions + key).toPersistentSet(), actionError = null) }
            try {
                block()
            } finally {
                _state.update { it.copy(loadingActions = (it.loadingActions - key).toPersistentSet()) }
            }
        }
    }

    private fun applySnapshot(snapshot: HeroSnapshot) {
        updateHero {
            copy(
                level = snapshot.level,
                currentXp = snapshot.currentXp,
                maxXp = snapshot.xpForNextLevel,
                currentHp = snapshot.currentHp,
                maxHp = snapshot.maxHp,
                gold = snapshot.gold,
                isDead = snapshot.isDead,
                deathCount = snapshot.deathCount,
                dailyCompletions = snapshot.dailyCompletions,
                dailyCompletionsMax = snapshot.dailyCompletionsMax,
                xpBoostPercent = snapshot.xpBoostPercent,
                xpBoostTasksRemaining = snapshot.xpBoostTasksRemaining,
            )
        }
    }

    fun refreshTasks() {
        viewModelScope.launch { doRefreshTasks() }
    }

    private suspend fun doRefreshTasks() {
        val id = heroId ?: return
        safeCall { taskUseCases.getTasks(id) }
            .onSuccess { data ->
                _state.update { state ->
                    state.copy(tasks = data.toVisibleUiTasksPreservingActionState(state.tasks))
                }
            }.onFailure { Timber.w("Background task refresh failed: $it") }
    }

    private suspend fun <T> executeAction(action: suspend () -> DomainResult<T>): T? =
        safeCall(action).fold(
            onSuccess = { it },
            onFailure = { error ->
                _state.update { it.copy(actionError = error.toUiError()) }
                null
            },
        )

    private suspend fun fetchHero(): HeroDomain? {
        val result = safeCall { heroUseCases.getCurrentHero() }
        return result.fold(
            onSuccess = { hero ->
                if (hero != null) {
                    hero
                } else {
                    _state.update { it.copy(needsHeroCreation = true) }
                    null
                }
            },
            onFailure = { error ->
                _state.update { it.copy(criticalError = error.toUiError()) }
                null
            },
        )
    }

    private fun observeSyncWorker() {
        workManager
            .getWorkInfosForUniqueWorkFlow(SyncScheduler.WORK_NAME)
            .map { workInfos -> workInfos.any { it.state == WorkInfo.State.SUCCEEDED } }
            .distinctUntilChanged()
            .drop(1)
            .filter { succeeded -> succeeded }
            .onEach {
                Timber.d("SyncWorker succeeded — refreshing tasks")
                doRefreshTasks()
            }.launchIn(viewModelScope)
    }

    private fun List<GameTaskDomain>.toVisibleUiTasks() =
        filter { task -> !task.isCompleted || task.type == TaskType.Habit || task.type == TaskType.Daily }
            .map { task -> task.toUi() }
            .toPersistentList()

    private fun List<GameTaskDomain>.toVisibleUiTasksPreservingActionState(previous: List<TaskUi>) =
        filter { task -> !task.isCompleted || task.type == TaskType.Habit || task.type == TaskType.Daily }
            .map { task ->
                val ui = task.toUi()
                val old = previous.firstOrNull { it.id == ui.id }
                ui.copy(
                    pendingAction = old?.pendingAction,
                    actionError = old?.actionError,
                )
            }.toPersistentList()

    private suspend fun <T> safeCall(block: suspend () -> DomainResult<T>): DomainResult<T> =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Unexpected exception in safeCall")
            if (isDebug) throw e
            DomainResult.Failure(GameError.Unknown(e.message ?: "Unexpected error"))
        }
}
