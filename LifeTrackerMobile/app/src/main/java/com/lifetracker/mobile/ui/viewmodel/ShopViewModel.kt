package com.lifetracker.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifetracker.mobile.domain.model.onFailure
import com.lifetracker.mobile.domain.model.onSuccess
import com.lifetracker.mobile.domain.usecase.shop.ShopUseCases
import com.lifetracker.mobile.ui.mapper.toUi
import com.lifetracker.mobile.ui.mapper.toUiError
import com.lifetracker.mobile.ui.model.HeroUi
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
    private var loadedItemsHeroId: Int = -1
    private var loadedInventoryHeroId: Int = -1
    private var currentHeroGold: Int = 0

    private object ActionKeys {
        fun buy(itemId: Int) = "buy_$itemId"
    }

    fun loadForHero(heroId: Int) {
        timber.log.Timber.d("loadForHero called: heroId=$heroId, currentHeroId=$currentHeroId")
        if (heroId <= 0) {
            timber.log.Timber.w("loadForHero: invalid heroId=$heroId, skipping")
            return
        }

        currentHeroId = heroId
        val shouldLoadItems = loadedItemsHeroId != heroId
        val shouldLoadInventory = loadedInventoryHeroId != heroId

        if (!shouldLoadItems && !shouldLoadInventory) {
            timber.log.Timber.d("loadForHero skipped: heroId=$heroId already loaded successfully")
            return
        }

        timber.log.Timber.d("loadForHero: loading items=$shouldLoadItems inventory=$shouldLoadInventory for heroId=$heroId")
        if (shouldLoadItems) loadItems(heroId)
        if (shouldLoadInventory) loadInventory(heroId)
    }

    fun showInventory(show: Boolean) {
        _state.update { it.copy(showInventory = show) }
        if (show && currentHeroId > 0 && loadedInventoryHeroId != currentHeroId) {
            loadInventory(currentHeroId)
        }
    }

    fun buyItem(
        heroId: Int,
        itemId: Int,
        hero: HeroUi,
        hasActiveShield: Boolean,
    ) {
        val key = ActionKeys.buy(itemId)
        viewModelScope.launch {
            _state.update { it.copy(actionError = null) }

            val item =
                _state.value.items
                    .firstOrNull { it.id == itemId }
                    ?: return@launch

            val guardMessage = getPurchaseGuardMessage(item.itemType, hero, hasActiveShield)
            if (guardMessage != null) {
                _events.send(UiEvent.ShowSnackbar(guardMessage))
                return@launch
            }

            val cost = item.cost
            val previousGold = if (currentHeroGold > 0) currentHeroGold else hero.gold
            val optimisticGold = (previousGold - cost).coerceAtLeast(0)
            currentHeroGold = optimisticGold
            _state.update { state ->
                state.copy(
                    items = state.items.map { it.copy(canAfford = optimisticGold >= it.cost) }.toPersistentList(),
                )
            }
            _events.send(UiEvent.HeroGoldUpdated(optimisticGold))

            shopUseCases
                .buyItem(heroId, itemId)
                .onSuccess { result ->
                    currentHeroGold = result.newGold
                    val updatedItems =
                        _state.value.items
                            .map { it.copy(canAfford = result.newGold >= it.cost) }
                            .toPersistentList()
                    _state.update { it.copy(items = updatedItems) }
                    _events.send(UiEvent.HeroGoldUpdated(result.newGold))
                    _events.send(UiEvent.HeroHpUpdated(result.newHp, result.maxHp))
                    _events.send(UiEvent.HeroXpBoostUpdated(result.xpBoostPercent, result.xpBoostTasksRemaining))
                    _events.send(UiEvent.HeroRecoveryUpdated(result.recoveryDebuffActive, result.recoveryMultiplier))
                    _events.send(UiEvent.ShowSnackbar(result.message))
                    viewModelScope.launch { loadInventory(currentHeroId) }
                }.onFailure { error ->
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
                inventory =
                    s.inventory
                        .map { inv ->
                            inv.copy(item = inv.item.copy(canAfford = heroGold >= inv.item.cost))
                        }.toPersistentList(),
            )
        }
    }

    fun dismissError() = _state.update { it.copy(actionError = null) }

    private fun getPurchaseGuardMessage(
        itemType: Int,
        hero: HeroUi,
        hasActiveShield: Boolean,
    ): String? =
        when {
            itemType in 1..2 && hero.currentHp >= hero.maxHp -> "HP is already full."
            itemType == 3 && hero.xpBoostPercent > 0 && hero.xpBoostTasksRemaining > 0 -> "XP Boost is already active."
            itemType == 4 && hasActiveShield -> "Shield is already active."
            itemType == 5 && !hero.isInRecovery -> "Revival Token is not needed right now."
            else -> null
        }

    private fun loadItems(heroId: Int) {
        viewModelScope.launch {
            timber.log.Timber.d("loadItems: starting to load shop items for heroId=$heroId")
            _state.update { it.copy(isLoadingItems = true) }
            shopUseCases
                .getShopItems()
                .onSuccess { items ->
                    timber.log.Timber.d("loadItems: received ${items.size} items from API")
                    loadedItemsHeroId = heroId
                    _state.update { s ->
                        s.copy(
                            items = items.map { it.toUi(currentHeroGold) }.toPersistentList(),
                            isLoadingItems = false,
                        )
                    }
                }.onFailure { error ->
                    timber.log.Timber.e("loadItems: failed to load items: $error")
                    _state.update { it.copy(isLoadingItems = false, actionError = error.toUiError()) }
                }
        }
    }

    private fun loadInventory(heroId: Int) {
        if (heroId <= 0) return
        viewModelScope.launch {
            _state.update { it.copy(isLoadingInventory = true) }
            shopUseCases
                .getInventory(heroId)
                .onSuccess { inventory ->
                    loadedInventoryHeroId = heroId
                    _state.update { s ->
                        s.copy(
                            inventory = inventory.map { it.toUi(currentHeroGold) }.toPersistentList(),
                            isLoadingInventory = false,
                        )
                    }
                }.onFailure { error ->
                    _state.update { it.copy(isLoadingInventory = false, actionError = error.toUiError()) }
                }
        }
    }
}
