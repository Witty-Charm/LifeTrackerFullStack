package com.lifetracker.mobile.repository

import com.lifetracker.mobile.data.Hero
import com.lifetracker.mobile.data.dao.HeroDao
import kotlinx.coroutines.flow.Flow

class HeroRepository(
    private val heroDao: HeroDao
) {
    val hero: Flow<Hero?> = heroDao.getHero()

    suspend fun updateHero(hero: Hero) {
        heroDao.insertHero(hero)
    }


}

