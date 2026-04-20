package com.lifetracker.mobile.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShopItemDto(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("description") val description: String,
    @SerialName("price") val price: Int,
    @SerialName("itemType") val itemType: Int,
    @SerialName("effectValue") val effectValue: Int,
)

@Serializable
data class BuyItemRequestDto(
    @SerialName("heroId") val heroId: Int,
    @SerialName("itemId") val itemId: Int,
    @SerialName("clientTimeZone") val clientTimeZone: String? = null,
    @SerialName("clientLocalDateTime") val clientLocalDateTime: String? = null,
)

@Serializable
data class BuyResultDto(
    @SerialName("newGold") val newGold: Int,
    @SerialName("newHp") val newHp: Int,
    @SerialName("maxHp") val maxHp: Int,
    @SerialName("purchasedItem") val purchasedItem: ShopItemDto,
    @SerialName("message") val message: String,
    @SerialName("effect") val effect: String = "",
    @SerialName("xpBoostPercent") val xpBoostPercent: Int = 0,
    @SerialName("xpBoostTasksRemaining") val xpBoostTasksRemaining: Int = 0,
    @SerialName("recoveryDebuffActive") val recoveryDebuffActive: Boolean = false,
    @SerialName("recoveryMultiplier") val recoveryMultiplier: Double = 1.0,
)

@Serializable
data class PurchasedItemDto(
    @SerialName("purchaseId") val purchaseId: Int,
    @SerialName("item") val item: ShopItemDto,
    @SerialName("purchasedAt") val purchasedAt: String,
)
