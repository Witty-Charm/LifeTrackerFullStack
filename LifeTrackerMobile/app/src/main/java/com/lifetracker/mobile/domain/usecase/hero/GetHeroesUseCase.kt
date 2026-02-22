package com.lifetracker.mobile.domain.usecase.hero

import com.lifetracker.mobile.domain.model.DomainResult
import com.lifetracker.mobile.domain.model.HeroDomain
import com.lifetracker.mobile.domain.repository.HeroRepository

class GetHeroesUseCase(
    private val repository: HeroRepository
) {
    suspend operator fun invoke(): DomainResult<List<HeroDomain>> =
        repository.getHeroes()
}