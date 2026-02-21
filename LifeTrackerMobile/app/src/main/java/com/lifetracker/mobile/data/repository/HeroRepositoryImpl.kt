package com.lifetracker.mobile.data.repository

import com.lifetracker.mobile.core.network.SafeApiCaller
import com.lifetracker.mobile.core.network.map
import com.lifetracker.mobile.data.mapper.toDomain
import com.lifetracker.mobile.data.mapper.toDomainResult
import com.lifetracker.mobile.data.remote.LifeTrackerApi
import com.lifetracker.mobile.data.remote.dto.CreateHeroRequest
import com.lifetracker.mobile.domain.model.DomainResult
import com.lifetracker.mobile.domain.model.HealResult
import com.lifetracker.mobile.domain.model.HeroDomain
import com.lifetracker.mobile.domain.model.HeroStatsDomain
import com.lifetracker.mobile.domain.model.RespawnResult
import com.lifetracker.mobile.domain.repository.HeroRepository

class HeroRepositoryImpl(
    private val api: LifeTrackerApi,
    private val caller: SafeApiCaller
) : HeroRepository {
    override suspend fun getHeroes(): DomainResult<List<HeroDomain>> =
        caller.safeApiCall { api.getHeroes() }
            .map { list -> list.map { it.toDomain() } }
            .toDomainResult()

    override suspend fun getHero(id: Int): DomainResult<HeroDomain> =
        caller.safeApiCall { api.getHero(id) }
            .map { it.toDomain() }
            .toDomainResult()

    override suspend fun getFirstHero(): DomainResult<HeroDomain?> =
        caller.safeApiCall { api.getHeroes() }
            .map { it.firstOrNull()?.toDomain() }
            .toDomainResult()

    override suspend fun createHero(
        name: String,
        startingGold: Int?,
    ): DomainResult<HeroDomain> =
        caller.safeApiCall {
            api.createHero(CreateHeroRequest(name = name, startingGold = startingGold))
        }.map { it.toDomain() }
            .toDomainResult()

    override suspend fun getHeroStats(heroId: Int): DomainResult<HeroStatsDomain> =
        caller.safeApiCall { api.getHeroStats(heroId) }
            .map { it.toDomain() }
            .toDomainResult()

    override suspend fun respawnHero(heroId: Int): DomainResult<RespawnResult> =
        caller.safeApiCall { api.respawnHero(heroId) }
            .map { it.toDomain() }
            .toDomainResult()

    override suspend fun healHero(
        heroId: Int,
        amount: Int?,
    ): DomainResult<HealResult> =
        caller.safeApiCall { api.healHero(heroId, amount) }
            .map { it.toDomain() }
            .toDomainResult()
}