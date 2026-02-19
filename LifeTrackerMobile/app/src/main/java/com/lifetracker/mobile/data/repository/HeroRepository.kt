package com.lifetracker.mobile.data.repository

import com.lifetracker.mobile.core.network.NetworkResult
import com.lifetracker.mobile.core.network.SafeApiCaller
import com.lifetracker.mobile.core.network.map
import com.lifetracker.mobile.data.mapper.toDomain
import com.lifetracker.mobile.data.remote.LifeTrackerApi
import com.lifetracker.mobile.data.remote.dto.CreateHeroRequest
import com.lifetracker.mobile.data.remote.dto.HeroUpdateBody
import com.lifetracker.mobile.domain.model.HealResult
import com.lifetracker.mobile.domain.model.HeroDomain
import com.lifetracker.mobile.domain.model.HeroStatsDomain
import com.lifetracker.mobile.domain.model.RespawnResult

class HeroRepository(
    private val api: LifeTrackerApi,
    private val caller: SafeApiCaller
) {
    suspend fun getHeroes(): NetworkResult<List<HeroDomain>> =
        caller.safeApiCall { api.getHeroes() }
            .map { list -> list.map { it.toDomain() } }

    suspend fun getHero(id: Int): NetworkResult<HeroDomain> =
        caller.safeApiCall { api.getHero(id) }
            .map { it.toDomain() }

    suspend fun getFirstHero(): NetworkResult<HeroDomain?> =
        caller.safeApiCall { api.getHeroes() }
            .map { it.firstOrNull()?.toDomain() }

    suspend fun createHero(
        name: String,
        startingGold: Int? = null,
    ): NetworkResult<HeroDomain> =
        caller.safeApiCall {
            api.createHero(
                CreateHeroRequest(
                    name = name,
                    startingGold = startingGold
                )
            )
        }.map { it.toDomain() }

    suspend fun updateHero(body: HeroUpdateBody): NetworkResult<Unit> =
        caller.safeApiCallUnit { api.updateHero(body.id, body) }

    suspend fun getHeroStats(heroId: Int): NetworkResult<HeroStatsDomain> =
        caller.safeApiCall { api.getHeroStats(heroId) }
            .map { it.toDomain() }

    suspend fun respawnHero(heroId: Int): NetworkResult<RespawnResult> =
        caller.safeApiCall { api.respawnHero(heroId) }
            .map { it.toDomain() }

    suspend fun healHero(
        heroId: Int,
        amount: Int? = null,
    ): NetworkResult<HealResult> =
        caller.safeApiCall { api.healHero(heroId, amount) }
            .map { it.toDomain() }
}