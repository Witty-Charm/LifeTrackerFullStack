package com.lifetracker.mobile.domain.usecase.shop

import com.lifetracker.mobile.domain.model.DomainResult
import com.lifetracker.mobile.domain.model.InventoryItemDomain
import com.lifetracker.mobile.domain.repository.ShopRepository

class GetInventoryUseCase(private val repository: ShopRepository) {
    suspend operator fun invoke(heroId: Int): DomainResult<List<InventoryItemDomain>> =
        repository.getInventory(heroId)
}
