package com.lifetracker.mobile.domain.usecase

import com.lifetracker.mobile.domain.model.DomainResult
import com.lifetracker.mobile.domain.model.HeroDomain
import com.lifetracker.mobile.domain.repository.HeroRepository

class CreateHeroUseCase(
    private val repository: HeroRepository
) {
    suspend operator fun invoke(name: String, startingGold: Int? = null): DomainResult<HeroDomain> =
        repository.createHero(name, startingGold)
}