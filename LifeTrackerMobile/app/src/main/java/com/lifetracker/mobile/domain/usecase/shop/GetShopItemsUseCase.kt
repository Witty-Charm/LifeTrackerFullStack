package com.lifetracker.mobile.domain.usecase.shop

import com.lifetracker.mobile.domain.model.DomainResult
import com.lifetracker.mobile.domain.model.ShopItemDomain
import com.lifetracker.mobile.domain.repository.ShopRepository

class GetShopItemsUseCase(private val repository: ShopRepository) {
    suspend operator fun invoke(): DomainResult<List<ShopItemDomain>> =
        repository.getShopItems()
}
