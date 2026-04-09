package com.lifetracker.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifetracker.mobile.domain.model.onFailure
import com.lifetracker.mobile.domain.model.onSuccess
import com.lifetracker.mobile.domain.usecase.shop.ShopUseCases
import com.lifetracker.mobile.ui.mapper.toUi
import com.lifetracker.mobile.ui.mapper.toUiError
import com.lifetracker.mobile.ui.model.ShopScreenState
import com.lifetracker.mobile.ui.model.UiEvent
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ShopViewModel(
    private val shopUseCases: ShopUseCases,
) : ViewModel() {

    private val _state = MutableStateFlow(ShopScreenState())
    val state: StateFlow<ShopScreenState> = _state.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var currentHeroId: Int = -1
    private var currentHeroGold: Int = 0

    private object ActionKeys {
        fun buy(itemId: Int) = "buy_$itemId"
    }

    fun loadForHero(heroId: Int) {
        if (heroId == currentHeroId || heroId <= 0) return
        currentHeroId = heroId
        loadItems()
        loadInventory()
    }

    fun showInventory(show: Boolean) {
        _state.update { it.copy(showInventory = show) }
        if (show && currentHeroId > 0) loadInventory()
    }

    fun buyItem(heroId: Int, itemId: Int, heroGold: Int) {
        val key = ActionKeys.buy(itemId)
        viewModelScope.launch {
            _state.update { it.copy(actionError = null) }

            val cost = _state.value.items.firstOrNull { it.id == itemId }?.cost ?: 0
            val previousGold = if (currentHeroGold > 0) currentHeroGold else heroGold
            val optimisticGold = (previousGold - cost).coerceAtLeast(0)
            currentHeroGold = optimisticGold
            _state.update { state ->
                state.copy(
                    items = state.items.map { it.copy(canAfford = optimisticGold >= it.cost) }.toPersistentList()
                )
            }
            _events.send(UiEvent.HeroGoldUpdated(optimisticGold))

            shopUseCases.buyItem(heroId, itemId)
                .onSuccess { result ->
                    currentHeroGold = result.newGold
                    val updatedItems = _state.value.items
                        .map { it.copy(canAfford = result.newGold >= it.cost) }
                        .toPersistentList()
                    _state.update { it.copy(items = updatedItems) }
                    _events.send(UiEvent.HeroGoldUpdated(result.newGold))
                    _events.send(UiEvent.HeroHpUpdated(result.newHp, result.maxHp))
                    _events.send(UiEvent.HeroXpBoostUpdated(result.xpBoostPercent, result.xpBoostTasksRemaining))
                    _events.send(UiEvent.ShowSnackbar(result.message))
                    viewModelScope.launch { loadInventory() }
                }
                .onFailure { error ->
                    _events.send(UiEvent.HeroGoldUpdated(previousGold))
                    _state.update { it.copy(actionError = error.toUiError()) }
                }
        }
    }

    fun refreshWithGold(heroGold: Int) {
        if (heroGold == currentHeroGold) return
        currentHeroGold = heroGold
        _state.update { s ->
            s.copy(
                items = s.items.map { it.copy(canAfford = heroGold >= it.cost) }.toPersistentList(),
                inventory = s.inventory.map { inv ->
                    inv.copy(item = inv.item.copy(canAfford = heroGold >= inv.item.cost))
                }.toPersistentList(),
            )
        }
    }

    fun dismissError() = _state.update { it.copy(actionError = null) }

    private fun loadItems() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingItems = true) }
            shopUseCases.getShopItems()
                .onSuccess { items ->
                    _state.update { s ->
                        s.copy(
                            items = items.map { it.toUi(currentHeroGold) }.toPersistentList(),
                            isLoadingItems = false,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoadingItems = false, actionError = error.toUiError()) }
                }
        }
    }

    private fun loadInventory() {
        if (currentHeroId <= 0) return
        viewModelScope.launch {
            _state.update { it.copy(isLoadingInventory = true) }
            shopUseCases.getInventory(currentHeroId)
                .onSuccess { inventory ->
                    _state.update { s ->
                        s.copy(
                            inventory = inventory.map { it.toUi(currentHeroGold) }.toPersistentList(),
                            isLoadingInventory = false,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoadingInventory = false, actionError = error.toUiError()) }
                }
        }
    }
}
