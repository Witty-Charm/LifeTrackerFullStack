package com.lifetracker.mobile.domain.usecase

import com.lifetracker.mobile.domain.model.DomainResult
import com.lifetracker.mobile.domain.model.TaskFailureResult
import com.lifetracker.mobile.domain.repository.TaskRepository

class FailTaskUseCase(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(taskId: Int): DomainResult<TaskFailureResult> =
        taskRepository.failTask(taskId)
}