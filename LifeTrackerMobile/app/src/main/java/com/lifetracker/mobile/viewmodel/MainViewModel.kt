package com.lifetracker.mobile.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lifetracker.mobile.api.ApiClient
import com.lifetracker.mobile.data.GameTask
import com.lifetracker.mobile.data.Hero
import com.lifetracker.mobile.data.database.AppDatabase
import com.lifetracker.mobile.repository.HeroRepository
import com.lifetracker.mobile.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val heroDao = database.heroDao()
    private val taskDao = database.taskDao()
    private val heroRepository = HeroRepository(heroDao)
    private val taskRepository = TaskRepository(
        taskDao = taskDao,
        heroDao = heroDao,
        taskApi = ApiClient.taskApi,
        heroApi = ApiClient.heroApi
    )
    
    val hero: Flow<Hero?> = heroRepository.hero
    val tasks: Flow<List<GameTask>> = taskRepository.activeTasks

    init {
        viewModelScope.launch {
            try {
                val h = ApiClient.heroApi.getHero()
                android.util.Log.d("MY_APP", "ГЕРОЙ ПРИШЕЛ: ${h.name}, XP: ${h.xp}")
                heroRepository.updateHero(h)
            } catch (e: Exception) {
                android.util.Log.e("MY_APP", "ГЕРОЙ НЕ ПРИШЕЛ: ${e.message}")
            }
            taskRepository.syncAllUnsyncedTasks()
        }
    }

    fun addTask(title: String, description: String?, rewardXp: Int) {
        viewModelScope.launch {
            try {
                val task = GameTask(
                    title = title,
                    description = description,
                    rewardXp = rewardXp,
                    isCompleted = false,
                    isSynced = false
                )
                taskRepository.addTask(task)
                android.util.Log.d("MY_APP", "Задача успешно добавлена в Room!")
            } catch (e: Exception) {
                android.util.Log.e("MY_APP", "КРАШ ТУТ: ${e.message}", e)
            }
        }
    }


    fun completeTask(task: GameTask) {
        viewModelScope.launch {
            try {
                taskRepository.completeTask(task)

                val sId = task.serverId?.toInt() ?: task.localId.toInt()
                taskRepository.completeAndSync(sId)

                android.util.Log.d("MY_APP", "XP начислен через сервер!")
            } catch (e: Exception) {
                android.util.Log.e("MY_APP", "Ошибка при выполнении: ${e.message}")
            }
        }
    }
}

