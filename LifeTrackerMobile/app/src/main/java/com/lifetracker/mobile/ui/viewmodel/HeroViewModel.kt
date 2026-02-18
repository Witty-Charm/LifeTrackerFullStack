package com.lifetracker.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifetracker.mobile.core.network.NetworkResult
import com.lifetracker.mobile.core.network.fold
import com.lifetracker.mobile.data.mapper.toDomain
import com.lifetracker.mobile.data.remote.dto.CreateTaskRequest
import com.lifetracker.mobile.data.repository.HeroRepository
import com.lifetracker.mobile.data.repository.TaskRepository
import com.lifetracker.mobile.domain.model.HeroDomain
import com.lifetracker.mobile.domain.model.HeroSnapshot
import com.lifetracker.mobile.ui.mapper.toUi
import com.lifetracker.mobile.ui.mapper.toUiError
import com.lifetracker.mobile.ui.model.HeroScreenState
import com.lifetracker.mobile.ui.model.UiError
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
            _state.update { it.copy(isLoading = true, criticalError = null) }

            val hero = fetchHero() ?: run {
                _state.update { it.copy(isLoading = false) }
                return@launch
            }

            val tasksDeferred = async { executeAction { taskRepo.getTasks(hero.id) } }
            val overdueDeferred = async { executeAction { taskRepo.checkOverdueTasks(hero.id) } }

            val tasks = tasksDeferred.await()
            val overdue = overdueDeferred.await()

            _state.update { current ->
                current.copy(
                    heroDomain = hero,
                    hero = hero.toUi(),
                    // tasks == null, with error - hero updates, tasks will be old
                    tasks = tasks?.map { it.toUi() } ?: current.tasks,
                    isLoading = false,
                )
            }

            if (overdue != null && overdue.overdueCount > 0) {
                _events.send(UiEvent.ShowSnackbar(overdue.message))
                refreshTasks()
            }
        }
    }

    fun completeTask(taskId: Int) {
        viewModelScope.launch {
            executeAction { taskRepo.completeTask(taskId) }
                ?.let { result ->
                    applySnapshot(result.heroSnapshot)
                    refreshTasks()
                    _events.send(UiEvent.TaskCompleted(result))
                }
        }
    }

    fun failTask(taskId: Int) {
        viewModelScope.launch {
            executeAction { taskRepo.failTask(taskId) }
                ?.let { result ->
                    applySnapshot(result.heroSnapshot)
                    refreshTasks()
                    _events.send(UiEvent.TaskFailed(result))
                }
        }
    }

    fun createTask(request: CreateTaskRequest) {
        viewModelScope.launch {
            executeAction { taskRepo.createTask(request) }
                ?.let { task ->
                    _state.update { current ->
                        current.copy(tasks = current.tasks + task.toUi())
                    }
                    _events.send(UiEvent.ShowSnackbar("Task '${task.title}' created!"))
                }
        }
    }

    fun deleteTask(taskId: Int) {
        viewModelScope.launch {
            executeAction { taskRepo.deleteTask(taskId) }
                ?.let {
                    _state.update { current ->
                        current.copy(tasks = current.tasks.filter { it.id != taskId })
                    }
                }
        }
    }

    fun respawnHero() {
        val heroId = _state.value.heroDomain?.id ?: return
        viewModelScope.launch {
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
        }
    }

    fun healHero(amount: Int? = null) {
        val heroId = _state.value.heroDomain?.id ?: return
        viewModelScope.launch {
            executeAction { heroRepo.healHero(heroId, amount) }
                ?.let { result ->
                    updateHero {
                        copy(
                            currentHp = result.newHp,
                            gold = result.newGold,
                        )
                    }
                    _events.send(UiEvent.HeroHealed(result.message))
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
        action: suspend () -> NetworkResult<T>,
    ): T? {
        return safeCall(action).fold(
            onSuccess = { it },
            onError = { _, apiError ->
                val uiError = apiError.toDomain().toUiError()
                if (isCritical) {
                    _state.update { it.copy(isLoading = false, criticalError = uiError) }
                } else {
                    _state.update { it.copy(actionError = uiError) }
                }
                null
            },
            onException = {
                val error = UiError.Network
                if (isCritical) {
                    _state.update { it.copy(isLoading = false, criticalError = error) }
                } else {
                    _state.update { it.copy(actionError = error) }
                }
                null
            },
        )
    }

    private suspend fun fetchHero(): HeroDomain? =
        executeAction(isCritical = true) { heroRepo.getFirstHero() }

    private suspend fun <T> safeCall(
        block: suspend () -> NetworkResult<T>,
    ): NetworkResult<T> = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e, "Unhandled exception in network call")
        NetworkResult.Exception(e)
    }
}