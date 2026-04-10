package com.lifetracker.mobile.data.repository

import com.lifetracker.mobile.core.network.SafeApiCaller
import com.lifetracker.mobile.core.network.dataOrNull
import com.lifetracker.mobile.core.network.map
import com.lifetracker.mobile.data.mapper.toDomain
import com.lifetracker.mobile.data.mapper.toDomainResult
import com.lifetracker.mobile.data.remote.LifeTrackerApi
import com.lifetracker.mobile.data.remote.dto.BuyItemRequestDto
import com.lifetracker.mobile.domain.model.BuyResult
import com.lifetracker.mobile.domain.model.DomainResult
import com.lifetracker.mobile.domain.model.InventoryItemDomain
import com.lifetracker.mobile.domain.model.ShopItemDomain
import com.lifetracker.mobile.domain.repository.ShopRepository
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class ShopRepositoryImpl(
    private val api: LifeTrackerApi,
    private val caller: SafeApiCaller,
) : ShopRepository {

    override suspend fun getShopItems(): DomainResult<List<ShopItemDomain>> =
        caller.safeApiCall { api.getShopItems() }
            .map { list -> list.map { it.toDomain() } }
            .toDomainResult()

    override suspend fun buyItem(heroId: Int, itemId: Int): DomainResult<BuyResult> {
        val shopItemsResult = caller.safeApiCall { api.getShopItems() }
        val isShield = shopItemsResult.dataOrNull()
            ?.find { it.id == itemId }
            ?.itemType == 4

        val request = if (isShield) {
            val zoneId = ZoneId.systemDefault()
            val zonedDateTime = ZonedDateTime.now(zoneId)
            val isoDateTime = zonedDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

            BuyItemRequestDto(
                heroId = heroId,
                itemId = itemId,
                clientTimeZone = zoneId.id,
                clientLocalDateTime = isoDateTime
            )
        } else {
            BuyItemRequestDto(heroId = heroId, itemId = itemId)
        }

        return caller.safeApiCall { api.buyItem(request) }
            .map { it.toDomain() }
            .toDomainResult()
    }

    override suspend fun getInventory(heroId: Int): DomainResult<List<InventoryItemDomain>> =
        caller.safeApiCall { api.getInventory(heroId) }
            .map { list -> list.map { it.toDomain() } }
            .toDomainResult()
}
