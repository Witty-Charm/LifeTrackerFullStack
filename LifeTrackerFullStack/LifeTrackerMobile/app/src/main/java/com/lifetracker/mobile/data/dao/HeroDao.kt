package com.lifetracker.mobile.data.dao

import androidx.room.*
import com.lifetracker.mobile.data.Hero
import kotlinx.coroutines.flow.Flow

@Dao
interface HeroDao {
    @Query("SELECT * FROM heroes LIMIT 1")
    fun getHero(): Flow<Hero?>
    
    @Query("SELECT * FROM heroes LIMIT 1")
    suspend fun getHeroSync(): Hero?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHero(hero: Hero)
    
    @Update
    suspend fun updateHero(hero: Hero)
    
    @Query("DELETE FROM heroes")
    suspend fun deleteAll()
}

