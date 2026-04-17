package com.lifetracker.mobile.domain.usecase.hero

import com.lifetracker.mobile.domain.model.DomainResult
import com.lifetracker.mobile.domain.model.GameError
import com.lifetracker.mobile.domain.repository.HeroRepository

class UpdateHeroTimeZoneUseCase(
    private val repository: HeroRepository,
) {
    suspend operator fun invoke(heroId: Int, timeZoneId: String): DomainResult<Unit> {
        if (timeZoneId.isBlank()) {
            return DomainResult.Failure(
                GameError.Validation(mapOf("timeZoneId" to listOf("Timezone cannot be empty")))
            )
        }

        return repository.updateHeroTimeZone(heroId, timeZoneId)
    }
}
