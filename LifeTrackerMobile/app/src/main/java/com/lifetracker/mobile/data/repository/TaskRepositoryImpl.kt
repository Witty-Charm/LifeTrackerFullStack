package com.lifetracker.mobile.data.repository

import com.lifetracker.mobile.core.network.SafeApiCaller
import com.lifetracker.mobile.core.network.map
import com.lifetracker.mobile.data.mapper.toDomain
import com.lifetracker.mobile.data.mapper.toDomainResult
import com.lifetracker.mobile.data.mapper.toDto
import com.lifetracker.mobile.data.remote.LifeTrackerApi
import com.lifetracker.mobile.data.remote.dto.CreateTaskRequest
import com.lifetracker.mobile.domain.model.CreateTaskParams
import com.lifetracker.mobile.domain.model.DomainResult
import com.lifetracker.mobile.domain.model.GameTaskDomain
import com.lifetracker.mobile.domain.model.OverdueResult
import com.lifetracker.mobile.domain.model.TaskCompletionResult
import com.lifetracker.mobile.domain.model.TaskFailureResult
import com.lifetracker.mobile.domain.repository.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TaskRepositoryImpl(
    private val api: LifeTrackerApi,
    private val caller: SafeApiCaller
) : TaskRepository {
    override suspend fun getTasks(heroId: Int): DomainResult<List<GameTaskDomain>> =
        caller.safeApiCall { api.getTasks(heroId) }
            .map { list -> list.map { it.toDomain() } }
            .toDomainResult()

    override suspend fun getTask(id: Int): DomainResult<GameTaskDomain> =
        caller.safeApiCall { api.getTask(id) }
            .map { it.toDomain() }
            .toDomainResult()

    override suspend fun createTask(params: CreateTaskParams): DomainResult<GameTaskDomain> =
        caller.safeApiCall {
            api.createTask(
                CreateTaskRequest(
                    heroId = params.heroId,
                    title = params.title,
                    description = params.description,
                    type = params.type.toDto(),
                    difficulty = params.difficulty.toDto(),
                    dueDate = params.dueDate,
                )
            )
        }.map { it.toDomain() }
            .toDomainResult()

    override suspend fun completeTask(taskId: Int): DomainResult<TaskCompletionResult> =
        caller.safeApiCall { api.completeTask(taskId) }
            .map { it.toDomain() }
            .toDomainResult()

    override suspend fun failTask(taskId: Int): DomainResult<TaskFailureResult> =
        caller.safeApiCall { api.failTask(taskId) }
            .map { it.toDomain() }
            .toDomainResult()

    override suspend fun checkOverdueTasks(heroId: Int): DomainResult<OverdueResult> =
        caller.safeApiCall { api.checkOverdueTasks(heroId) }
            .map { it.toDomain() }
            .toDomainResult()

    override suspend fun deleteTask(taskId: Int): DomainResult<Unit> =
        caller.safeApiCallUnit { api.deleteTask(taskId) }
            .toDomainResult()
}
