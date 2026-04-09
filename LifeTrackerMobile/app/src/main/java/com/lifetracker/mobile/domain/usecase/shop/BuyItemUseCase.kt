package com.lifetracker.mobile.domain.usecase.shop

import com.lifetracker.mobile.domain.model.BuyResult
import com.lifetracker.mobile.domain.model.DomainResult
import com.lifetracker.mobile.domain.repository.ShopRepository

class BuyItemUseCase(private val repository: ShopRepository) {
    suspend operator fun invoke(heroId: Int, itemId: Int): DomainResult<BuyResult> =
        repository.buyItem(heroId, itemId)
}
