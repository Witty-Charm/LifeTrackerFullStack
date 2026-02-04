package com.lifetracker.mobile.repository

import com.lifetracker.mobile.api.ApiClient
import com.lifetracker.mobile.api.HeroApi
import com.lifetracker.mobile.api.TaskApi
import com.lifetracker.mobile.data.GameTask
import com.lifetracker.mobile.data.dao.HeroDao
import com.lifetracker.mobile.data.dao.TaskDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TaskRepository(
    private val taskDao: TaskDao,
    private val heroDao: HeroDao,
    private val taskApi: TaskApi,
    private val heroApi: HeroApi
) {
    val activeTasks: Flow<List<GameTask>> = taskDao.getActiveTasks()
    suspend fun completeTask(task: GameTask) {
        withContext(Dispatchers.IO) {
            taskDao.updateTask(task.copy(isCompleted = true))
        }
    }
    suspend fun addTask(task: GameTask) {
        withContext(Dispatchers.IO) {
            val taskId = taskDao.insertTask(task.copy(isSynced = false))

            syncTask(task.copy(localId = taskId))
        }
    }

    suspend fun syncTask(task: GameTask) {
        withContext(Dispatchers.IO) {
            try {
                val response = taskApi.createTask(task)
                if (response.isSuccessful) {
                    val syncedTask = response.body()
                    if (syncedTask != null) {
                        taskDao.updateTask(
                            task.copy(
                                serverId = syncedTask.serverId ?: syncedTask.localId.toLong(),
                                isSynced = true
                            )
                        )

                        val updatedHero = heroApi.getHero()
                        heroDao.insertHero(updatedHero)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    suspend fun syncAllUnsyncedTasks() {
        withContext(Dispatchers.IO) {
            val unsyncedTasks = taskDao.getUnsyncedTasks()
            unsyncedTasks.forEach { task ->
                syncTask(task)
            }
        }
    }
    suspend fun completeAndSync(taskId: Int) {
        withContext(Dispatchers.IO) {
            try {
                val response = taskApi.completeTask(taskId)
                if (response.isSuccessful) {
                    val updatedHero = heroApi.getHero()
                    heroDao.insertHero(updatedHero)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


}

