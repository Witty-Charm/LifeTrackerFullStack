package com.lifetracker.mobile.domain.usecase.task

import com.lifetracker.mobile.domain.model.DomainResult
import com.lifetracker.mobile.domain.model.TaskFailureResult
import com.lifetracker.mobile.domain.repository.TaskRepository

class FailTaskUseCase(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(taskId: Int): DomainResult<TaskFailureResult> =
        repository.failTask(taskId)
}