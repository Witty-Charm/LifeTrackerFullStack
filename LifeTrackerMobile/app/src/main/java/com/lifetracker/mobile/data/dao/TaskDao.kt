package com.lifetracker.mobile.data.dao

import androidx.room.*
import com.lifetracker.mobile.data.GameTask
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY localId DESC")
    fun getActiveTasks(): Flow<List<GameTask>>
    
    @Query("SELECT * FROM tasks WHERE isSynced = 0")
    suspend fun getUnsyncedTasks(): List<GameTask>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: GameTask): Long
    
    @Update
    suspend fun updateTask(task: GameTask)
    
    @Delete
    suspend fun deleteTask(task: GameTask)
    
    @Query("SELECT * FROM tasks WHERE localId = :id")
    suspend fun getTaskById(id: Long): GameTask?
}

