package com.lifetracker.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.lifetracker.mobile.BuildConfig
import com.lifetracker.mobile.core.sync.SyncScheduler
import com.lifetracker.mobile.domain.model.CreateTaskParams
import com.lifetracker.mobile.domain.model.DomainResult
import com.lifetracker.mobile.domain.model.GameError
import com.lifetracker.mobile.domain.model.GameTaskDomain
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
import com.lifetracker.mobile.ui.model.UiDifficulty
import com.lifetracker.mobile.ui.model.UiEvent
import com.lifetracker.mobile.ui.model.UiTaskType
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
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
    }

    private object ActionKeys {
        const val HERO_CREATE = "hero_create"
        const val HERO_RESPAWN = "hero_respawn"
        const val HERO_HEAL = "hero_heal"
        const val TASK_CREATE = "task_create"

        fun taskComplete(id: Int) = "task_complete_$id"

        fun taskFail(id: Int) = "task_fail_$id"

        fun taskDelete(id: Int) = "task_delete_$id"
    }

    private val _state = MutableStateFlow(HeroScreenState())
    val state: StateFlow<HeroScreenState> = _state.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var loadJob: Job? = null
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
            if (isPendingSync(taskId)) {
                _events.send(UiEvent.ShowSnackbar("Task is not synced yet. Try again when online."))
                return@launchAction
            }
            executeAction { taskUseCases.completeTask(taskId) }
                ?.let { result ->
                    applySnapshot(result.heroSnapshot)
                    doRefreshTasks()
                    _events.send(
                        UiEvent.TaskAction(
                            TaskActionFeedback.Completed(
                                xpGained = result.xpGained,
                                goldGained = result.goldGained,
                                leveledUp = result.leveledUp,
                                newLevel = result.newLevel.takeIf { result.leveledUp },
                            ),
                        ),
                    )
                }
        }

    fun failTask(taskId: Int) =
        launchAction(ActionKeys.taskFail(taskId)) {
            if (isPendingSync(taskId)) {
                _events.send(UiEvent.ShowSnackbar("Task is not synced yet. Try again when online."))
                return@launchAction
            }
            executeAction { taskUseCases.failTask(taskId) }
                ?.let { result ->
                    applySnapshot(result.heroSnapshot)
                    doRefreshTasks()
                    _events.send(
                        UiEvent.TaskAction(
                            TaskActionFeedback.Failed(
                                hpLost = result.damageDealt,
                                goldLost = result.goldLost,
                                shieldAbsorbed = result.shieldAbsorbed && !result.streakBroken,
                            ),
                        ),
                    )
                }
        }

    fun createTask(
        title: String,
        description: String?,
        type: UiTaskType,
        difficulty: UiDifficulty,
        dueDate: kotlin.time.Instant?,
    ) = launchAction(ActionKeys.TASK_CREATE) {
        val id = heroId ?: return@launchAction
        val params =
            CreateTaskParams(
                heroId = id,
                title = title,
                description = description,
                type = type.toDomain(),
                difficulty = difficulty.toDomain(),
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

    fun deleteTask(taskId: Int) =
        launchAction(ActionKeys.taskDelete(taskId)) {
            executeAction { taskUseCases.deleteTask(taskId) }
                ?.let {
                    _state.update { current ->
                        current.copy(tasks = current.tasks.filter { it.id != taskId }.toPersistentList())
                    }
                }
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

    private fun isPendingSync(taskId: Int): Boolean =
        _state.value.tasks
            .find { it.id == taskId }
            ?.isPendingSync == true

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
                _state.update { state -> state.copy(tasks = data.toVisibleUiTasks()) }
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
        val result = safeCall { heroUseCases.getFirstHero() }
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
