package com.lifetracker.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifetracker.mobile.domain.model.DomainResult
import com.lifetracker.mobile.domain.model.GameError
import com.lifetracker.mobile.domain.model.dataOrNull
import com.lifetracker.mobile.domain.model.errorOrNull
import com.lifetracker.mobile.domain.usecase.hero.HeroUseCases
import com.lifetracker.mobile.domain.usecase.task.TaskUseCases
import com.lifetracker.mobile.ui.mapper.toStatsUiState
import com.lifetracker.mobile.ui.mapper.toUiError
import com.lifetracker.mobile.ui.model.StatsScreenState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class StatsViewModel(
    private val heroUseCases: HeroUseCases,
    private val taskUseCases: TaskUseCases,
) : ViewModel() {
    private val _state = MutableStateFlow(StatsScreenState())
    val state: StateFlow<StatsScreenState> = _state.asStateFlow()
    private var loadStatsJob: Job? = null

    fun loadStats(heroId: Int) {
        if (heroId <= 0) return

        loadStatsJob?.cancel()
        loadStatsJob =
            viewModelScope.launch {
                _state.update { it.copy(isLoading = true, actionError = null) }

                try {
                    val (heroStatsResult, tasksResult) =
                        coroutineScope {
                            val heroStatsDeferred = async { safeCall { heroUseCases.getHeroStats(heroId) } }
                            val tasksDeferred = async { safeCall { taskUseCases.getTasks(heroId) } }
                            heroStatsDeferred.await() to tasksDeferred.await()
                        }

                    val error = heroStatsResult.errorOrNull() ?: tasksResult.errorOrNull()
                    if (error != null) {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                actionError = error.toUiError(),
                            )
                        }
                        return@launch
                    }

                    val heroStats = heroStatsResult.dataOrNull() ?: return@launch _state.update { current -> current.copy(isLoading = false) }
                    val tasks = tasksResult.dataOrNull().orEmpty()

                    _state.value = heroStats.toStatsUiState(tasks = tasks).copy(isLoading = false, actionError = null)
                } finally {
                    if (loadStatsJob === kotlinx.coroutines.currentCoroutineContext()[Job]) {
                        _state.update { current -> if (current.isLoading) current.copy(isLoading = false) else current }
                        loadStatsJob = null
                    }
                }
            }
    }

    fun dismissError() {
        _state.update { it.copy(actionError = null) }
    }

    private suspend fun <T> safeCall(block: suspend () -> DomainResult<T>): DomainResult<T> =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Unexpected exception in stats safeCall")
            DomainResult.Failure(GameError.Unknown(e.message ?: "Unexpected error"))
        }
}
