package com.lifetracker.mobile.domain.usecase.hero

import com.lifetracker.mobile.domain.model.DomainResult
import com.lifetracker.mobile.domain.model.GameError
import com.lifetracker.mobile.domain.model.HeroDomain
import com.lifetracker.mobile.domain.repository.HeroRepository

class CreateHeroUseCase(
    private val repository: HeroRepository
) {
    suspend operator fun invoke(name: String, startingGold: Int? = null): DomainResult<HeroDomain> {
        val errors = buildMap {
            if (name.isBlank())
                put("name", listOf("Name cannot be empty"))
            else if (name.length > 50)
                put("name", listOf("Name cannot exceed 50 characters"))
        }

        if (errors.isNotEmpty())
            return DomainResult.Failure(GameError.Validation(errors))

        return repository.createHero(name, startingGold)
    }
}