package com.lifetracker.mobile.domain.usecase

import com.lifetracker.mobile.domain.model.DomainResult
import com.lifetracker.mobile.domain.model.TaskCompletionResult
import com.lifetracker.mobile.domain.repository.TaskRepository

class CompleteTaskUseCase(
    private val taskRepository: TaskRepository,
) {
    suspend operator fun invoke(taskId: Int): DomainResult<TaskCompletionResult> =
        taskRepository.completeTask(taskId)
}