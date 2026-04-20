package com.lifetracker.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifetracker.mobile.domain.model.onFailure
import com.lifetracker.mobile.domain.model.onSuccess
import com.lifetracker.mobile.domain.usecase.hero.HeroUseCases
import com.lifetracker.mobile.ui.mapper.toUi
import com.lifetracker.mobile.ui.mapper.toUiError
import com.lifetracker.mobile.ui.model.AchievementsScreenState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AchievementsViewModel(
    private val heroUseCases: HeroUseCases,
) : ViewModel() {
    private val _state = MutableStateFlow(AchievementsScreenState())
    val state: StateFlow<AchievementsScreenState> = _state.asStateFlow()

    fun loadAchievements(heroId: Int) {
        if (heroId <= 0) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, actionError = null) }
            heroUseCases
                .getHeroAchievements(heroId)
                .onSuccess { achievements ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            achievements = achievements.toUi(),
                            actionError = null,
                        )
                    }
                }.onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            actionError = error.toUiError(),
                        )
                    }
                }
        }
    }

    fun dismissError() {
        _state.update { it.copy(actionError = null) }
    }
}
