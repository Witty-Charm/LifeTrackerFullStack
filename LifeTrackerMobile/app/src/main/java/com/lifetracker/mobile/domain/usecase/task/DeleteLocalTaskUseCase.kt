package com.lifetracker.mobile.domain.usecase.task

import com.lifetracker.mobile.domain.repository.TaskRepository

class DeleteLocalTaskUseCase(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(taskId: Int) = taskRepository.deleteLocalTask(taskId)
}