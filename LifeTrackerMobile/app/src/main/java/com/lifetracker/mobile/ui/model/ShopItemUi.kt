package com.lifetracker.mobile.ui.model

data class ShopItemUi(
    val id: Int,
    val name: String,
    val description: String,
    val cost: Int,
    val itemType: Int,
    val effectValue: Int,
    val canAfford: Boolean,
)

data class InventoryItemUi(
    val purchaseId: Int,
    val item: ShopItemUi,
    val purchasedAt: String,
)
