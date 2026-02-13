package com.lifetracker.mobile.data.repository

import com.lifetracker.mobile.core.network.NetworkResult
import com.lifetracker.mobile.core.network.map
import com.lifetracker.mobile.core.network.safeApiCall
import com.lifetracker.mobile.data.remote.LifeTrackerApi
import com.lifetracker.mobile.data.remote.dto.CompleteTaskResponse
import com.lifetracker.mobile.data.remote.dto.CreateTaskRequest
import com.lifetracker.mobile.data.remote.dto.FailTaskResponse
import com.lifetracker.mobile.data.remote.dto.OverdueCheckResponse
import com.lifetracker.mobile.data.remote.dto.TaskDto
import com.lifetracker.mobile.domain.mapper.toDomain
import com.lifetracker.mobile.domain.model.GameTaskDomain


class TaskRepository(private val api: LifeTrackerApi) {
    suspend fun getTasks(): NetworkResult<List<GameTaskDomain>> =
        safeApiCall { api.getTasks() }
            .map { list -> list.map { it.toDomain() } }

    suspend fun getTask(("id") id: Int): Response<TaskDto>

    suspend fun createTask( request: CreateTaskRequest): Response<TaskDto>

    suspend fun completeTask(("id") id: Int): Response<CompleteTaskResponse>

    suspend fun failTask(("id") id: Int): Response<FailTaskResponse>

    suspend fun checkOverdueTasks(
        ("heroId") heroId: Int? = null,
    ): Response<OverdueCheckResponse>

    suspend fun deleteTask(("id") id: Int): Response<Unit>
}