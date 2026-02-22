package com.lifetracker.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifetracker.mobile.domain.model.CreateTaskParams
import com.lifetracker.mobile.domain.model.DomainResult
import com.lifetracker.mobile.domain.model.GameError
import com.lifetracker.mobile.domain.model.HeroDomain
import com.lifetracker.mobile.domain.model.HeroSnapshot
import com.lifetracker.mobile.domain.model.dataOrNull
import com.lifetracker.mobile.domain.model.errorOrNull
import com.lifetracker.mobile.domain.model.fold
import com.lifetracker.mobile.domain.model.onFailure
import com.lifetracker.mobile.domain.model.onSuccess
import com.lifetracker.mobile.domain.usecase.hero.HeroUseCases
import com.lifetracker.mobile.domain.usecase.task.TaskUseCases
import com.lifetracker.mobile.ui.mapper.toUi
import com.lifetracker.mobile.ui.mapper.toUiError
import com.lifetracker.mobile.ui.model.HeroScreenState
import com.lifetracker.mobile.ui.model.UiEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import timber.log.Timber

class HeroViewModel(
    private val heroUseCases: HeroUseCases,
    private val taskUseCases: TaskUseCases,
    private val isDebug: Boolean = false,
) : ViewModel() {

    private val _state = MutableStateFlow(HeroScreenState())
    val state: StateFlow<HeroScreenState> = _state.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var loadJob: Job? = null

    private var heroDomain: HeroDomain? = null
    private val heroId: Int? get() = heroDomain?.id

    init {
        loadData()
    }

    fun loadData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, criticalError = null, actionError = null) }

            val hero = fetchHero() ?: run {
                return@launch
            }

            try {
                supervisorScope {
                    heroDomain = hero

                    val tasksDefered = async { safeCall { taskUseCases.getTasks(hero.id) } }
                    val overdueDefered = async { safeCall { taskUseCases.checkOverdue(hero.id) } }
                    val overdue = overdueDefered.await()
                    val hasOverdue = overdue.dataOrNull()?.let { it.overdueCount > 0 } == true

                    val tasks = if (hasOverdue) {
                        tasksDefered.cancel()
                        safeCall { taskUseCases.getTasks(hero.id) }
                    } else {
                        tasksDefered.await()
                    }

                    _state.update { current ->
                        current.copy(
                            hero = hero.toUi(),
                            tasks = tasks.dataOrNull()?.map { it.toUi() } ?: current.tasks,
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
                }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun completeTask(taskId: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionError = null) }
            try {
                executeAction { taskUseCases.completeTask(taskId) }
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
                executeAction { taskUseCases.failTask(taskId) }
                    ?.let { result ->
                        applySnapshot(result.heroSnapshot)
                        refreshTasks()
                        _events.send(UiEvent.TaskFailed(result))
                    }
            } finally {
                _state.update { it.copy(isActionLoading = false) }
            }
        }
    }

    fun createTask(params: CreateTaskParams) {
        val id = heroId ?: return
        val paramsWithHero = params.copy(heroId = id)
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionError = null) }
            try {
                executeAction { taskUseCases.createTask(paramsWithHero) }
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
                executeAction { taskUseCases.deleteTask(taskId) }
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
        val id = heroId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionError = null) }
            try {
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
                            )
                        )
                    }
            } finally {
                _state.update { it.copy(isActionLoading = false) }
            }
        }
    }

    fun healHero(amount: Int? = null) {
        val id = heroId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionError = null) }
            try {
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
            } finally {
                _state.update { it.copy(isActionLoading = false) }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(criticalError = null, actionError = null) }
    }

    private fun updateHero(transform: HeroDomain.() -> HeroDomain) {
        val updated = heroDomain?.transform() ?: return
        heroDomain = updated
        _state.update { it.copy(hero = updated.toUi()) }
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
            )
        }
    }

    private suspend fun refreshTasks() {
        val id = heroId ?: return
        safeCall { taskUseCases.getTasks(id) }
            .onSuccess { data ->
                _state.update { it.copy(tasks = data.map { t -> t.toUi() }) }
            }
            .onFailure { Timber.w("Background task refresh failed: $it") }
    }

    private suspend fun <T> executeAction(
        action: suspend () -> DomainResult<T>,
    ): T? {
        return safeCall(action).fold(
            onSuccess = { it },
            onFailure = { error ->
                _state.update { it.copy(actionError = error.toUiError()) }
                null
            },
        )
    }

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
                _state.update { it.copy(isLoading = false, criticalError = error.toUiError()) }
                null
            },
        )
    }

    private suspend fun <T> safeCall(
        block: suspend () -> DomainResult<T>,
    ): DomainResult<T> = try {
        block()
    } catch (e: CancellationException) { throw e }
      catch (e: java.io.IOException) {
        Timber.w(e, "Network exception")
        DomainResult.Failure(GameError.Network)
    }
      catch (e: Exception) {
        Timber.e(e, "Unexpected exception in safeCall")
        if (isDebug) throw e
        DomainResult.Failure(GameError.Unknown(e.message ?: "Unexpected error"))
    }
}