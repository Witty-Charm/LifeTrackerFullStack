package com.lifetracker.mobile.data.repository

import com.lifetracker.mobile.core.network.SafeApiCaller
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

class ShopRepositoryImpl(
    private val api: LifeTrackerApi,
    private val caller: SafeApiCaller,
) : ShopRepository {

    override suspend fun getShopItems(): DomainResult<List<ShopItemDomain>> =
        caller.safeApiCall { api.getShopItems() }
            .map { list -> list.map { it.toDomain() } }
            .toDomainResult()

    override suspend fun buyItem(heroId: Int, itemId: Int): DomainResult<BuyResult> =
        caller.safeApiCall { api.buyItem(BuyItemRequestDto(heroId, itemId)) }
            .map { it.toDomain() }
            .toDomainResult()

    override suspend fun getInventory(heroId: Int): DomainResult<List<InventoryItemDomain>> =
        caller.safeApiCall { api.getInventory(heroId) }
            .map { list -> list.map { it.toDomain() } }
            .toDomainResult()
}
