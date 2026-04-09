package com.lifetracker.mobile.domain.repository

import com.lifetracker.mobile.domain.model.BuyResult
import com.lifetracker.mobile.domain.model.DomainResult
import com.lifetracker.mobile.domain.model.InventoryItemDomain
import com.lifetracker.mobile.domain.model.ShopItemDomain

interface ShopRepository {
    suspend fun getShopItems(): DomainResult<List<ShopItemDomain>>
    suspend fun buyItem(heroId: Int, itemId: Int): DomainResult<BuyResult>
    suspend fun getInventory(heroId: Int): DomainResult<List<InventoryItemDomain>>
}
