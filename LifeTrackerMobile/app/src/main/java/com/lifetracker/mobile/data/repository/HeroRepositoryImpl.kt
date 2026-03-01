package com.lifetracker.mobile.data.repository

import com.lifetracker.mobile.core.network.SafeApiCaller
import com.lifetracker.mobile.core.network.map
import com.lifetracker.mobile.data.local.dao.HeroDao
import com.lifetracker.mobile.data.mapper.toDomain
import com.lifetracker.mobile.data.mapper.toDomainResult
import com.lifetracker.mobile.data.mapper.toEntity
import com.lifetracker.mobile.data.remote.LifeTrackerApi
import com.lifetracker.mobile.data.remote.dto.CreateHeroRequest
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
        val remote = caller.safeApiCall { api.getHeroes() }
            .map { list -> list.map { it.toDomain() } }
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
        val remote = caller.safeApiCall { api.getHero(id) }
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

    override suspend fun getFirstHero(): DomainResult<HeroDomain?> {
        val remote = caller.safeApiCall { api.getHeroes() }
            .map { it.firstOrNull()?.toDomain() }
            .toDomainResult()

        return when (remote) {
            is DomainResult.Success -> {
                remote.data?.let { heroDao.upsert(it.toEntity()) }
                remote
            }
            is DomainResult.Failure -> {
                val local = heroDao.getFirst()?.toDomain()
                if (local != null) DomainResult.Success(local) else remote
            }
        }
    }

    override suspend fun createHero(
        name: String,
        startingGold: Int?,
    ): DomainResult<HeroDomain> {
        val remote = caller.safeApiCall {
            api.createHero(CreateHeroRequest(name = name, startingGold = startingGold))
        }.map { it.toDomain() }.toDomainResult()

        if (remote is DomainResult.Success) {
            heroDao.upsert(remote.data.toEntity())
        }
        return remote
    }

    override suspend fun getHeroStats(heroId: Int): DomainResult<HeroStatsDomain> =
        caller.safeApiCall { api.getHeroStats(heroId) }
            .map { it.toDomain() }
            .toDomainResult()

    override suspend fun respawnHero(heroId: Int): DomainResult<RespawnResult> {
        val remote = caller.safeApiCall { api.respawnHero(heroId) }
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
                    )
                )
            }
        }
        return remote
    }

    override suspend fun healHero(
        heroId: Int,
        amount: Int?,
    ): DomainResult<HealResult> {
        val remote = caller.safeApiCall { api.healHero(heroId, amount) }
            .map { it.toDomain() }
            .toDomainResult()

        if (remote is DomainResult.Success) {
            heroDao.getById(heroId)?.let { entity ->
                heroDao.upsert(
                    entity.copy(
                        currentHp = remote.data.newHp,
                        maxHp = remote.data.maxHp,
                        gold = remote.data.newGold,
                    )
                )
            }
        }
        return remote
    }
}
