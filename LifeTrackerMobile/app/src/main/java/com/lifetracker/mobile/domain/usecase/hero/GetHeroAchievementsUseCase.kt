package com.lifetracker.mobile.domain.usecase.hero

import com.lifetracker.mobile.domain.model.AchievementDomain
import com.lifetracker.mobile.domain.model.DomainResult
import com.lifetracker.mobile.domain.repository.HeroRepository

class GetHeroAchievementsUseCase(
    private val repository: HeroRepository,
) {
    suspend operator fun invoke(heroId: Int): DomainResult<List<AchievementDomain>> = repository.getHeroAchievements(heroId)
}
