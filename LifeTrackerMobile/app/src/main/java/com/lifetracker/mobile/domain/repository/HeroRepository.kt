package com.lifetracker.mobile.domain.repository

import com.lifetracker.mobile.domain.model.DomainResult
import com.lifetracker.mobile.domain.model.HealResult
import com.lifetracker.mobile.domain.model.HeroDomain
import com.lifetracker.mobile.domain.model.HeroStatsDomain
import com.lifetracker.mobile.domain.model.RespawnResult

interface HeroRepository {
    suspend fun getHeroes(): DomainResult<List<HeroDomain>>
    suspend fun getHero(id: Int): DomainResult<HeroDomain>
    suspend fun getFirstHero(): DomainResult<HeroDomain?>
    suspend fun createHero(name: String, startingGold: Int? = null): DomainResult<HeroDomain>
    suspend fun getHeroStats(heroId: Int): DomainResult<HeroStatsDomain>
    suspend fun respawnHero(heroId: Int): DomainResult<RespawnResult>
    suspend fun healHero(heroId: Int, amount: Int? = null): DomainResult<HealResult>
}