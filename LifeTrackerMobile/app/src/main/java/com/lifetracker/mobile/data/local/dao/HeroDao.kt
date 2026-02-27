package com.lifetracker.mobile.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.lifetracker.mobile.data.local.entity.HeroEntity

@Dao
interface HeroDao {
    @Query("SELECT * FROM heroes")
    suspend fun getAll(): List<HeroEntity>

    @Query("SELECT * FROM heroes WHERE id = :id")
    suspend fun getById(id: Int): HeroEntity?

    @Upsert
    suspend fun upsert(hero: HeroEntity)
}