package com.lifetracker.mobile.data.repository

import com.lifetracker.mobile.core.network.NetworkResult
import com.lifetracker.mobile.core.network.map
import com.lifetracker.mobile.core.network.safeApiCall
import com.lifetracker.mobile.core.network.safeApiCallUnit
import com.lifetracker.mobile.data.remote.LifeTrackerApi
import com.lifetracker.mobile.data.remote.dto.CreateTaskRequest
import com.lifetracker.mobile.data.remote.dto.TaskDifficulty
import com.lifetracker.mobile.data.remote.dto.TaskType
import com.lifetracker.mobile.domain.mapper.toDomain
import com.lifetracker.mobile.domain.model.GameTaskDomain
import com.lifetracker.mobile.domain.model.OverdueResult
import com.lifetracker.mobile.domain.model.TaskCompletionResult
import com.lifetracker.mobile.domain.model.TaskFailureResult
import kotlin.time.Instant

class TaskRepository(private val api: LifeTrackerApi) {
    suspend fun getTasks(): NetworkResult<List<GameTaskDomain>> =
        safeApiCall { api.getTasks() }
            .map { list -> list.map { it.toDomain() } }

    suspend fun getTask(id: Int): NetworkResult<GameTaskDomain> =
        safeApiCall { api.getTask(id) }
            .map { it.toDomain() }

    suspend fun createTask(request: CreateTaskRequest): NetworkResult<GameTaskDomain> =
        safeApiCall { api.createTask(request) }
            .map { it.toDomain() }

    suspend fun completeTask(taskId: Int): NetworkResult<TaskCompletionResult> =
        safeApiCall { api.completeTask(taskId) }
            .map { it.toDomain() }

    suspend fun failTask(taskId: Int): NetworkResult<TaskFailureResult> =
        safeApiCall { api.failTask(taskId) }
            .map { it.toDomain() }

    suspend fun checkOverdueTasks(heroId: Int): NetworkResult<OverdueResult> =
        safeApiCall { api.checkOverdueTasks(heroId) }
            .map { it.toDomain() }

    suspend fun deleteTask(taskId: Int): NetworkResult<Unit> =
        safeApiCallUnit { api.deleteTask(taskId) }
    }
