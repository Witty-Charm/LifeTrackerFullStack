package com.lifetracker.mobile.domain.repository

import com.lifetracker.mobile.domain.model.*

interface TaskRepository {
    suspend fun getTasks(heroId: Int): DomainResult<List<GameTaskDomain>>
    suspend fun getTask(id: Int): DomainResult<GameTaskDomain>
    suspend fun createTask(params: CreateTaskParams): DomainResult<GameTaskDomain>
    suspend fun completeTask(taskId: Int): DomainResult<TaskCompletionResult>
    suspend fun failTask(taskId: Int): DomainResult<TaskFailureResult>
    suspend fun checkOverdueTasks(heroId: Int): DomainResult<OverdueResult>
    suspend fun deleteTask(taskId: Int): DomainResult<Unit>
}