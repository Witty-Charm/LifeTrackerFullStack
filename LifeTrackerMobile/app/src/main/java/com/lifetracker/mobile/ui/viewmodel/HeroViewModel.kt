package com.lifetracker.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifetracker.mobile.core.network.NetworkResult
import com.lifetracker.mobile.core.network.onSuccess
import com.lifetracker.mobile.data.repository.HeroRepository
import com.lifetracker.mobile.data.repository.TaskRepository
import com.lifetracker.mobile.domain.model.HeroDomain
import com.lifetracker.mobile.core.network.fold
import com.lifetracker.mobile.ui.mapper.toUi
import com.lifetracker.mobile.ui.model.HeroScreenState
import com.lifetracker.mobile.ui.model.UiError
import com.lifetracker.mobile.ui.model.UiEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import com.lifetracker.mobile.core.network.onFailure
import com.lifetracker.mobile.data.mapper.toDomain
import com.lifetracker.mobile.data.remote.dto.CreateTaskRequest
import com.lifetracker.mobile.ui.mapper.toUiError
import timber.log.Timber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
            _state.update { it.copy(isLoading = true, error = null) }

            val hero = fetchHero() ?: return@launch

            val tasksDeferred = async { executeAction { taskRepo.getTasks(hero.id) } }
            val overdueDeferred = async { executeAction { taskRepo.checkOverdueTasks(hero.id) } }

            val tasks = tasksDeferred.await()

            _state.update { current ->
                current.copy(
                    hero = hero.toUi(),
                    tasks = tasks?.map { it.toUi() } ?: current.tasks,
                    isLoading = false,
                )
            }

            val overdue = overdueDeferred.await()
            if (overdue != null && overdue.overdueCount > 0) {
                _events.send(UiEvent.ShowSnackbar(overdue.message))
                refreshTasks(hero.id)
            }
        }
    }

    fun completeTask(taskId: Int) {
        viewModelScope.launch {
            executeAction { taskRepo.completeTask(taskId) }
                ?.let { data ->
                    _events.send(
                        UiEvent.TaskCompleted(
                            taskTitle = data.taskTitle,
                            xpGained = data.xpGained,
                            goldGained = data.goldGained,
                            leveledUp = data.leveledUp,
                            newLevel = data.newLevel,
                            message = data.message,
                        )
                    )
                    loadData()
                }
        }
    }

    fun failTask(taskId: Int) {
        viewModelScope.launch {
            executeAction { taskRepo.failTask(taskId) }
                ?.let { data ->
                    _events.send(
                        UiEvent.TaskFailed(
                            taskTitle = data.taskTitle,
                            heroDied = data.heroDied,
                            message = data.message,
                        )
                    )
                    loadData()
                }
        }
    }

    fun createTask(request: CreateTaskRequest) {
        viewModelScope.launch {
            executeAction { taskRepo.createTask(request) }
                ?.let { data ->
                    _events.send(UiEvent.ShowSnackbar("Task '${data.title}' created!"))
                    loadData()
                }
        }
    }

    fun deleteTask(taskId: Int) {
        viewModelScope.launch {
            executeAction { taskRepo.deleteTask(taskId) }
            loadData()
        }
    }

    fun respawnHero() {
        val heroId = _state.value.hero?.id ?: return
        viewModelScope.launch {
            executeAction { heroRepo.respawnHero(heroId) }
                ?.let { data ->
                    _events.send(UiEvent.HeroRespawned(data.message))
                    loadData()
                }
        }
    }

    fun healHero(amount: Int? = null) {
        val heroId = _state.value.hero?.id ?: return
        viewModelScope.launch {
            executeAction { heroRepo.healHero(heroId, amount) }
                ?.let { data ->
                    _events.send(UiEvent.HeroHealed(data.message))
                    loadData()
                }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private suspend fun <T> executeAction(
        action: suspend () -> NetworkResult<T>,
    ): T? {
        return safeCall(action).fold(
            onSuccess = { it },
            onError = { _, apiError ->
                _state.update { it.copy(error = apiError.toDomain().toUiError()) }
                null
            },
            onException = {
                _state.update { it.copy(isLoading = false, error = UiError.Network) }
                null
            },
        )
    }

    private suspend fun fetchHero(): HeroDomain? {
        return executeAction { heroRepo.getFirstHero() }
    }

    private suspend fun refreshTasks(heroId: Int) {
        safeCall { taskRepo.getTasks(heroId) }
            .onSuccess { data ->
                _state.update { it.copy(tasks = data.map { t -> t.toUi() }) }
            }
            .onFailure {
                Timber.w("Task refresh failed for heroId=%d", heroId)
            }
    }

    private suspend fun <T> safeCall(
        block: suspend () -> NetworkResult<T>,
    ): NetworkResult<T> {
        return try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Unhandled exception in network call")
            NetworkResult.Exception(e)
        }
    }
}