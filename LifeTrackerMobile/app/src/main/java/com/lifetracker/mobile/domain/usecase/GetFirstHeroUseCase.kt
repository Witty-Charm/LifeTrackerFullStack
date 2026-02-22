package com.lifetracker.mobile.domain.usecase

import com.lifetracker.mobile.domain.model.DomainResult
import com.lifetracker.mobile.domain.model.HeroDomain
import com.lifetracker.mobile.domain.repository.HeroRepository

class GetFirstHeroUseCase(
    private val repository: HeroRepository
) {
    suspend operator fun invoke(): DomainResult<HeroDomain?> =
        repository.getFirstHero()
}