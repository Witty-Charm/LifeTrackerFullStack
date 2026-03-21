package com.lifetracker.mobile.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.lifetracker.mobile.data.local.entity.TaskEntity

@Dao
abstract class TaskDao {
    @Query("SELECT * FROM tasks")
    abstract suspend fun getAll(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id")
    abstract suspend fun getById(id: Int): TaskEntity?

    @Query("SELECT * FROM tasks WHERE heroId = :heroId")
    abstract suspend fun getByHeroId(heroId: Int): List<TaskEntity>

    @Query("DELETE FROM tasks WHERE id = :id")
    abstract suspend fun deleteById(id: Int)

    @Query("SELECT * FROM tasks WHERE pendingSync = 1")
    abstract suspend fun getPendingSync(): List<TaskEntity>

    @Upsert
    abstract suspend fun upsert(task: TaskEntity)

    @Upsert
    abstract suspend fun upsertAll(tasks: List<TaskEntity>)

    @Query("SELECT COALESCE(MIN(id), 0) - 1 FROM tasks")
    abstract suspend fun getNextTempId(): Int

    @Transaction
    open suspend fun insertOfflineTask(task: TaskEntity): TaskEntity {
        val tempId = getNextTempId()
        val withId = task.copy(id = tempId)
        upsert(withId)
        return withId
    }

    @Transaction
    open suspend fun replaceTempWithReal(tempId: Int, realTask: TaskEntity) {
        upsert(realTask)
        deleteById(tempId)
    }
}
