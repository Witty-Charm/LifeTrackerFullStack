package com.lifetracker.mobile.data.repository

import com.lifetracker.mobile.core.network.SafeApiCaller
import com.lifetracker.mobile.core.network.map
import com.lifetracker.mobile.data.local.dao.HeroDao
import com.lifetracker.mobile.data.mapper.toDomain
import com.lifetracker.mobile.data.mapper.toDomainResult
import com.lifetracker.mobile.data.mapper.toEntity
import com.lifetracker.mobile.data.remote.LifeTrackerApi
import com.lifetracker.mobile.data.remote.dto.CreateHeroRequest
import com.lifetracker.mobile.data.remote.dto.UpdateHeroTimeZoneRequest
import com.lifetracker.mobile.domain.model.DomainResult
import com.lifetracker.mobile.domain.model.HealResult
import com.lifetracker.mobile.domain.model.HeroDomain
import com.lifetracker.mobile.domain.model.HeroStatsDomain
import com.lifetracker.mobile.domain.model.RespawnResult
import com.lifetracker.mobile.domain.repository.HeroRepository
import timber.log.Timber

class HeroRepositoryImpl(
    private val api: LifeTrackerApi,
    private val caller: SafeApiCaller,
    private val heroDao: HeroDao,
) : HeroRepository {
    override suspend fun getHeroes(): DomainResult<List<HeroDomain>> {
        val remote =
            caller
                .safeApiCall { api.getCurrentHero() }
                .map { hero -> listOf(hero.toDomain()) }
                .toDomainResult()

        return when (remote) {
            is DomainResult.Success -> {
                remote.data.forEach { heroDao.upsert(it.toEntity()) }
                remote
            }

            is DomainResult.Failure -> {
                val local = heroDao.getAll().map { it.toDomain() }
                if (local.isNotEmpty()) {
                    Timber.w("getHeroes: network failed, returning ${local.size} cached heroes")
                    DomainResult.Success(local)
                } else {
                    remote
                }
            }
        }
    }

    override suspend fun getHero(id: Int): DomainResult<HeroDomain> {
        val remote =
            caller
                .safeApiCall { api.getHero(id) }
                .map { it.toDomain() }
                .toDomainResult()

        return when (remote) {
            is DomainResult.Success -> {
                heroDao.upsert(remote.data.toEntity())
                remote
            }

            is DomainResult.Failure -> {
                val local = heroDao.getById(id)?.toDomain()
                if (local != null) DomainResult.Success(local) else remote
            }
        }
    }

    override suspend fun getCurrentHero(): DomainResult<HeroDomain?> {
        val remote =
            caller
                .safeApiCall { api.getCurrentHero() }
                .map { it.toDomain() }
                .toDomainResult()

        return when (remote) {
            is DomainResult.Success -> {
                heroDao.upsert(remote.data.toEntity())
                DomainResult.Success(remote.data)
            }

            is DomainResult.Failure -> {
                val local = heroDao.getFirst()?.toDomain()
                if (local != null) DomainResult.Success(local) else remote
            }
        }
    }

    override suspend fun getFirstHero(): DomainResult<HeroDomain?> = getCurrentHero()

    override suspend fun createHero(
        name: String,
        startingGold: Int?,
    ): DomainResult<HeroDomain> {
        val remote =
            caller
                .safeApiCall {
                    api.createHero(CreateHeroRequest(name = name, startingGold = startingGold))
                }.map { it.toDomain() }
                .toDomainResult()

        if (remote is DomainResult.Success) {
            heroDao.upsert(remote.data.toEntity())
        }
        return remote
    }

    override suspend fun getHeroStats(heroId: Int): DomainResult<HeroStatsDomain> =
        caller
            .safeApiCall { api.getHeroStats(heroId) }
            .map { it.toDomain() }
            .toDomainResult()

    override suspend fun getHeroAchievements(heroId: Int): DomainResult<List<com.lifetracker.mobile.domain.model.AchievementDomain>> =
        caller
            .safeApiCall { api.getHeroAchievements(heroId) }
            .map { it.toDomain() }
            .toDomainResult()

    override suspend fun respawnHero(heroId: Int): DomainResult<RespawnResult> {
        val remote =
            caller
                .safeApiCall { api.respawnHero(heroId) }
                .map { it.toDomain() }
                .toDomainResult()

        if (remote is DomainResult.Success) {
            heroDao.getById(heroId)?.let { entity ->
                heroDao.upsert(
                    entity.copy(
                        currentHp = remote.data.newHp,
                        maxHp = remote.data.maxHp,
                        isDead = false,
                        isInRecovery = remote.data.recoveryDebuffActive,
                        recoveryMultiplier = remote.data.recoveryMultiplier,
                        deathCount = remote.data.deathCount,
                    ),
                )
            }
        }
        return remote
    }

    override suspend fun healHero(
        heroId: Int,
        amount: Int?,
    ): DomainResult<HealResult> {
        val remote =
            caller
                .safeApiCall { api.healHero(heroId, amount) }
                .map { it.toDomain() }
                .toDomainResult()

        if (remote is DomainResult.Success) {
            heroDao.getById(heroId)?.let { entity ->
                heroDao.upsert(
                    entity.copy(
                        currentHp = remote.data.newHp,
                        maxHp = remote.data.maxHp,
                        gold = remote.data.newGold,
                    ),
                )
            }
        }
        return remote
    }

    override suspend fun updateHeroTimeZone(
        heroId: Int,
        timeZoneId: String,
    ): DomainResult<Unit> {
        val remote =
            caller.safeApiCallUnit {
                api.updateHeroTimeZone(heroId, UpdateHeroTimeZoneRequest(timeZoneId = timeZoneId))
            }

        return when (remote) {
            is com.lifetracker.mobile.core.network.NetworkResult.Success -> {
                DomainResult.Success(Unit)
            }

            is com.lifetracker.mobile.core.network.NetworkResult.Error -> {
                DomainResult.Failure(remote.apiError.toDomain())
            }

            is com.lifetracker.mobile.core.network.NetworkResult.Exception -> {
                DomainResult.Failure(
                    when (remote.throwable) {
                        is java.io.IOException -> {
                            com.lifetracker.mobile.domain.model.GameError.Network
                        }

                        else -> {
                            com.lifetracker.mobile.domain.model.GameError
                                .Unknown(remote.throwable.message ?: "Unexpected error")
                        }
                    },
                )
            }
        }
    }
}
