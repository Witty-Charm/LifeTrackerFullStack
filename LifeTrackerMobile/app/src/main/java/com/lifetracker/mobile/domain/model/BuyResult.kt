package com.lifetracker.mobile.domain.model

data class BuyResult(
    val newGold: Int,
    val newHp: Int,
    val maxHp: Int,
    val purchasedItem: ShopItemDomain,
    val message: String,
    val effect: String = "",
    val xpBoostPercent: Int = 0,
    val xpBoostTasksRemaining: Int = 0,
    val recoveryDebuffActive: Boolean = false,
    val recoveryMultiplier: Double = 1.0,
)
