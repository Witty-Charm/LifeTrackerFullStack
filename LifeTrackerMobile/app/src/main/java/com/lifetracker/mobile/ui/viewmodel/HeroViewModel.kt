package com.lifetracker.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifetracker.mobile.data.repository.HeroRepository
import com.lifetracker.mobile.data.repository.TaskRepository
import com.lifetracker.mobile.domain.model.CreateTaskParams
import com.lifetracker.mobile.domain.model.DomainResult
import com.lifetracker.mobile.domain.model.GameError
import com.lifetracker.mobile.domain.model.HeroDomain
import com.lifetracker.mobile.domain.model.HeroSnapshot
import com.lifetracker.mobile.domain.model.dataOrNull
import com.lifetracker.mobile.domain.model.errorOrNull
import com.lifetracker.mobile.domain.model.fold
import com.lifetracker.mobile.ui.mapper.toUi
import com.lifetracker.mobile.ui.mapper.toUiError
import com.lifetracker.mobile.ui.model.HeroScreenState
import com.lifetracker.mobile.ui.model.UiEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class HeroViewModel(
    private val heroRepo: HeroRepository,
    private val taskRepo: TaskRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HeroScreenState())
    val state: StateFlow<HeroScreenState> = _state.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events: Flow<UiEvent> = _events.receiveAsFlow()

    private var loadJob: Job? = null

    init {
        loadData()
    }

    fun loadData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, criticalError = null, actionError = null) }

            val hero = fetchHero() ?: run {
                _state.update { it.copy(isLoading = false) }
                return@launch
            }

            val tasksResult = async { safeCall { taskRepo.getTasks(hero.id) } }
            val overdueResult = async { safeCall { taskRepo.checkOverdueTasks(hero.id) } }
            val tasks = tasksResult.await()
            val overdue = overdueResult.await()

            _state.update { current ->
                val newTasks = tasks.dataOrNull()?.map { it.toUi() } ?: current.tasks
                val error = tasks.errorOrNull()?.toUiError()
                current.copy(
                    heroDomain = hero,
                    hero = hero.toUi(),
                    tasks = newTasks,
                    isLoading = false,
                    actionError = error,
                )
            }

            overdue.dataOrNull()?.let {
                if (it.overdueCount > 0) {
                    _events.send(UiEvent.ShowSnackbar(it.message))
                    refreshTasks()
                }
            }
            overdue.errorOrNull()?.let {
                Timber.w("checkOverdueTasks failed: $it")
            }
        }
    }

    fun completeTask(taskId: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionError = null) }
            try {
                executeAction { taskRepo.completeTask(taskId) }
                    ?.let { result ->
                        applySnapshot(result.heroSnapshot)
                        refreshTasks()
                        _events.send(UiEvent.TaskCompleted(result))
                    }
            } finally {
                _state.update { it.copy(isActionLoading = false) }
            }
        }
    }

    fun failTask(taskId: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionError = null) }
            try {
                executeAction { taskRepo.failTask(taskId) }
                    ?.let { result ->
                        applySnapshot(result.heroSnapshot)
                        if (result.heroDied) {
                            updateHero { copy(isDead = true) }
                        }
                        refreshTasks()
                        _events.send(UiEvent.TaskFailed(result))
                    }
            } finally {
                _state.update { it.copy(isActionLoading = false) }
            }
        }
    }

    fun createTask(params: CreateTaskParams) {
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionError = null) }
            try {
                executeAction { taskRepo.createTask(params) }
                    ?.let { task ->
                        _state.update { current ->
                            current.copy(tasks = current.tasks + task.toUi())
                        }
                        _events.send(UiEvent.ShowSnackbar("Task '${task.title}' created!"))
                    }
            } finally {
                _state.update { it.copy(isActionLoading = false) }
            }
        }
    }

    fun deleteTask(taskId: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionError = null) }
            try {
                executeAction { taskRepo.deleteTask(taskId) }
                    ?.let {
                        _state.update { current ->
                            current.copy(tasks = current.tasks.filter { it.id != taskId })
                        }
                    }
            } finally {
                _state.update { it.copy(isActionLoading = false) }
            }
        }
    }

    fun respawnHero() {
        val heroId = _state.value.heroDomain?.id ?: return
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionError = null) }
            try {
                executeAction { heroRepo.respawnHero(heroId) }
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
                            )
                        )
                    }
            } finally {
                _state.update { it.copy(isActionLoading = false) }
            }
        }
    }

    fun healHero(amount: Int? = null) {
        val heroId = _state.value.heroDomain?.id ?: return
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionError = null) }
            try {
                executeAction { heroRepo.healHero(heroId, amount) }
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
            } finally {
                _state.update { it.copy(isActionLoading = false) }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(criticalError = null, actionError = null) }
    }

    private fun updateHero(transform: HeroDomain.() -> HeroDomain) {
        _state.update { current ->
            val updated = current.heroDomain?.transform()
            current.copy(heroDomain = updated, hero = updated?.toUi())
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
                deathCount = snapshot.deathCount,
                dailyCompletions = snapshot.dailyCompletions,
                dailyCompletionsMax = snapshot.dailyCompletionsMax,
            )
        }
    }

    private suspend fun refreshTasks() {
        val heroId = _state.value.heroDomain?.id ?: return
        executeAction { taskRepo.getTasks(heroId) }
            ?.let { data ->
                _state.update { it.copy(tasks = data.map { t -> t.toUi() }) }
            }
    }

    private suspend fun <T> executeAction(
        isCritical: Boolean = false,
        action: suspend () -> DomainResult<T>,
    ): T? {
        return safeCall(action).fold(
            onSuccess = { it },
            onFailure = { error ->
                val uiError = error.toUiError()
                if (isCritical) {
                    _state.update { it.copy(isLoading = false, criticalError = uiError) }
                } else {
                    _state.update { it.copy(actionError = uiError) }
                }
                null
            },
        )
    }

    private suspend fun fetchHero(): HeroDomain? {
        val result = safeCall { heroRepo.getFirstHero() }

        return result.fold(
            onSuccess = { hero ->
                if (hero != null) {
                    hero
                } else {
                    _state.update { it.copy(isLoading = false, needsHeroCreation = true) }
                    null
                }
            },
            onFailure = { error ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        criticalError = error.toUiError()
                    )
                }
                null
            },
        )
    }

    private suspend fun <T> safeCall(
        block: suspend () -> DomainResult<T>,
    ): DomainResult<T> = try {
        block()
    } catch (e: CancellationException) { throw e }
      catch (e: Exception) {
        Timber.e(e, "Unhandled exception")
        DomainResult.Failure(GameError.Network)
    }
}