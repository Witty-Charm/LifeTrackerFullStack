package com.lifetracker.mobile.domain.usecase.hero

import com.lifetracker.mobile.domain.model.DomainResult
import com.lifetracker.mobile.domain.model.HeroStatsDomain
import com.lifetracker.mobile.domain.repository.HeroRepository

class GetHeroStatsUseCase(
    private val repository: HeroRepository
) {
    suspend operator fun invoke(heroId: Int): DomainResult<HeroStatsDomain> =
        repository.getHeroStats(heroId)
}