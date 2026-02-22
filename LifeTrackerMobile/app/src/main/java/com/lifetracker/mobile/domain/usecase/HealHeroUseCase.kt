package com.lifetracker.mobile.domain.usecase

import com.lifetracker.mobile.domain.model.DomainResult
import com.lifetracker.mobile.domain.model.HealResult
import com.lifetracker.mobile.domain.repository.HeroRepository

class HealHeroUseCase(
    private val repository: HeroRepository
) {
    suspend operator fun invoke(heroId: Int, amount: Int? = null): DomainResult<HealResult> =
        repository.healHero(heroId, amount)
}