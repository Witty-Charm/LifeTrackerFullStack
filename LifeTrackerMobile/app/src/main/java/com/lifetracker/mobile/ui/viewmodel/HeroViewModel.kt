package com.lifetracker.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifetracker.mobile.core.network.NetworkResult
import com.lifetracker.mobile.data.repository.HeroRepository
import com.lifetracker.mobile.data.repository.TaskRepository
import com.lifetracker.mobile.ui.mapper.toUi
import com.lifetracker.mobile.ui.model.HeroScreenState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HeroViewModel(
    private val heroRepo: HeroRepository,
    private val taskRepo: TaskRepository
) : ViewModel() {
    private val _state = MutableStateFlow(HeroScreenState())
    val state = _state.asStateFlow()

    fun loadHero() {
        viewModelScope.launch {
            val result = heroRepo.getFirstHero()

            if (result is NetworkResult.Success) {
                val heroData = result.data
                _state.update { currentScreenState ->
                    currentScreenState.copy(
                        hero = heroData.toUi(),
                        isLoading = false
                    )
                }
            }
        }
    }
}