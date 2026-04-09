package com.lifetracker.mobile.domain.usecase.shop

data class ShopUseCases(
    val getShopItems: GetShopItemsUseCase,
    val buyItem: BuyItemUseCase,
    val getInventory: GetInventoryUseCase,
)
