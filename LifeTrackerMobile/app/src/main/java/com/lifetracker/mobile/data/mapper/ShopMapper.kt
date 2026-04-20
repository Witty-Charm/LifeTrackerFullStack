package com.lifetracker.mobile.data.mapper

import com.lifetracker.mobile.data.remote.dto.BuyResultDto
import com.lifetracker.mobile.data.remote.dto.PurchasedItemDto
import com.lifetracker.mobile.data.remote.dto.ShopItemDto
import com.lifetracker.mobile.domain.model.BuyResult
import com.lifetracker.mobile.domain.model.InventoryItemDomain
import com.lifetracker.mobile.domain.model.ShopItemDomain
import kotlin.time.Instant

fun ShopItemDto.toDomain(): ShopItemDomain =
    ShopItemDomain(
        id = id,
        name = name,
        description = description,
        price = price,
        itemType = itemType,
        effectValue = effectValue,
    )

fun BuyResultDto.toDomain(): BuyResult =
    BuyResult(
        newGold = newGold,
        newHp = newHp,
        maxHp = maxHp,
        purchasedItem = purchasedItem.toDomain(),
        message = message,
        effect = effect,
        xpBoostPercent = xpBoostPercent,
        xpBoostTasksRemaining = xpBoostTasksRemaining,
        recoveryDebuffActive = recoveryDebuffActive,
        recoveryMultiplier = recoveryMultiplier,
    )

fun PurchasedItemDto.toDomain(): InventoryItemDomain =
    InventoryItemDomain(
        purchaseId = purchaseId,
        item = item.toDomain(),
        purchasedAt = runCatching { Instant.parse(purchasedAt) }.getOrElse { Instant.fromEpochMilliseconds(0) },
    )
