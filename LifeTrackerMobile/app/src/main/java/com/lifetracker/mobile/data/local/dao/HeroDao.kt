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

    @Query("SELECT * FROM heroes LIMIT 1")
    suspend fun getFirst(): HeroEntity?

    @Upsert
    suspend fun upsert(hero: HeroEntity)

    @Query("DELETE FROM heroes")
    suspend fun deleteAll()
}