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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

    // confined to Main thread via viewModelScope - do not read/write from IO/Default context
    private var heroDomain: HeroDomain? = null
    private val heroId: Int? get() = heroDomain?.id

    init {
        loadData()
    }

    fun loadData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    criticalError = null,
                    actionError = null,
                    needsHeroCreation = false
                )
            }

            val hero = fetchHero() ?: run {
                _state.update { it.copy(isLoading = false) }
                return@launch
            }

            try {
                heroDomain = hero

                val overdue = safeCall { taskUseCases.checkOverdue(hero.id) }
                val tasks = safeCall { taskUseCases.getTasks(hero.id) }

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

            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun completeTask(taskId: Int) = launchAction {
        executeAction { taskUseCases.completeTask(taskId) }
            ?.let { result ->
                applySnapshot(result.heroSnapshot)
                refreshTasks()
                _events.send(UiEvent.TaskCompleted(result))
            }
    }

    fun failTask(taskId: Int) = launchAction {
        executeAction { taskUseCases.failTask(taskId) }
            ?.let { result ->
                applySnapshot(result.heroSnapshot)
                refreshTasks()
                _events.send(UiEvent.TaskFailed(result))

            }
    }

    fun createTask(params: CreateTaskParams) = launchAction {
        val id = heroId ?: return@launchAction
        val paramsWithHero = params.copy(heroId = id)
        executeAction { taskUseCases.createTask(paramsWithHero) }
            ?.let { task ->
                _state.update { current ->
                    current.copy(tasks = current.tasks + task.toUi())
                }
                _events.send(UiEvent.ShowSnackbar("Task '${task.title}' created!"))
            }
    }

    fun deleteTask(taskId: Int) = launchAction {
        executeAction { taskUseCases.deleteTask(taskId) }
            ?.let {
                _state.update { current ->
                    current.copy(tasks = current.tasks.filter { it.id != taskId })
                }
            }

    }

    fun respawnHero() = launchAction {
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
                    )
                )
            }

    }

    fun healHero(amount: Int? = null) = launchAction {
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

    fun clearError() {
        _state.update { it.copy(criticalError = null, actionError = null) }
    }

    private fun updateHero(transform: HeroDomain.() -> HeroDomain) {
        val updated = heroDomain?.transform() ?: return
        heroDomain = updated
        _state.update { it.copy(hero = updated.toUi()) }
    }

    fun createHero(name: String, startingGold: Int? = null) = launchAction {
        executeAction { heroUseCases.createHero(name, startingGold) }
            ?.let { hero ->
                heroDomain = hero
                _state.update { it.copy(hero = hero.toUi(), needsHeroCreation = false) }
                refreshTasks()
            }

    }

    private fun launchAction(block: suspend () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionError = null) }
            try {
                block()
            } finally {
                _state.update { it.copy(isActionLoading = false) }
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
                if (hero != null) hero
                else {
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

    private suspend fun <T> safeCall(
        block: suspend () -> DomainResult<T>,
    ): DomainResult<T> = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e, "Unexpected exception in safeCall")
        if (isDebug) throw e
        DomainResult.Failure(GameError.Unknown(e.message ?: "Unexpected error"))
    }
}