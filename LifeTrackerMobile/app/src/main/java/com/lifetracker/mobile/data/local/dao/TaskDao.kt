package com.lifetracker.mobile.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.lifetracker.mobile.data.local.entity.TaskEntity

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks")
    suspend fun getAll(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Int): TaskEntity?

    @Query("SELECT * FROM tasks WHERE heroId = :heroId")
    suspend fun getByHeroId(heroId: Int): List<TaskEntity>

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT * FROM tasks WHERE pendingSync = 1")
    suspend fun getPendingSync(): List<TaskEntity>

    @Upsert
    suspend fun upsert(task: TaskEntity)

    @Upsert
    suspend fun upsertAll(tasks: List<TaskEntity>)
}


