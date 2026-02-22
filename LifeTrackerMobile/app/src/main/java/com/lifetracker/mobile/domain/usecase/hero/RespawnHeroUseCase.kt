package com.lifetracker.mobile.domain.usecase.hero

import com.lifetracker.mobile.domain.model.DomainResult
import com.lifetracker.mobile.domain.model.RespawnResult
import com.lifetracker.mobile.domain.repository.HeroRepository

class RespawnHeroUseCase(
    private val repository: HeroRepository
) {
    suspend operator fun invoke(heroId: Int): DomainResult<RespawnResult> =
        repository.respawnHero(heroId)
}