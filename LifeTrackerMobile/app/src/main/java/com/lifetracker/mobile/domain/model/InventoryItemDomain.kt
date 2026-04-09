package com.lifetracker.mobile.domain.model

import kotlin.time.Instant

data class InventoryItemDomain(
    val purchaseId: Int,
    val item: ShopItemDomain,
    val purchasedAt: Instant,
)
