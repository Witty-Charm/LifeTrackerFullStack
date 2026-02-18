package com.lifetracker.mobile.data.repository

import com.lifetracker.mobile.core.network.NetworkResult
import com.lifetracker.mobile.core.network.SafeApiCaller
import com.lifetracker.mobile.core.network.map
import com.lifetracker.mobile.data.remote.LifeTrackerApi
import com.lifetracker.mobile.data.remote.dto.CreateTaskRequest
import com.lifetracker.mobile.domain.mapper.toDomain
import com.lifetracker.mobile.domain.model.GameTaskDomain
import com.lifetracker.mobile.domain.model.OverdueResult
import com.lifetracker.mobile.domain.model.TaskCompletionResult
import com.lifetracker.mobile.domain.model.TaskFailureResult

class TaskRepository(
    private val api: LifeTrackerApi,
    private val caller: SafeApiCaller
) {
    suspend fun getTasks(heroId: Int): NetworkResult<List<GameTaskDomain>> =
        caller.safeApiCall { api.getTasks(heroId) }
            .map { list -> list.map { it.toDomain() } }

    suspend fun getTask(id: Int): NetworkResult<GameTaskDomain> =
        caller.safeApiCall { api.getTask(id) }
            .map { it.toDomain() }

    suspend fun createTask(request: CreateTaskRequest): NetworkResult<GameTaskDomain> =
        caller.safeApiCall { api.createTask(request) }
            .map { it.toDomain() }

    suspend fun completeTask(taskId: Int): NetworkResult<TaskCompletionResult> =
        caller.safeApiCall { api.completeTask(taskId) }
            .map { it.toDomain() }

    suspend fun failTask(taskId: Int): NetworkResult<TaskFailureResult> =
        caller.safeApiCall { api.failTask(taskId) }
            .map { it.toDomain() }

    suspend fun checkOverdueTasks(heroId: Int): NetworkResult<OverdueResult> =
        caller.safeApiCall { api.checkOverdueTasks(heroId) }
            .map { it.toDomain() }

    suspend fun deleteTask(taskId: Int): NetworkResult<Unit> =
        caller.safeApiCallUnit { api.deleteTask(taskId) }
    }
