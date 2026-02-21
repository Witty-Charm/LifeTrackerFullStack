package com.lifetracker.mobile.domain.usecase

import com.lifetracker.mobile.domain.model.CreateTaskParams
import com.lifetracker.mobile.domain.model.DomainResult
import com.lifetracker.mobile.domain.model.GameTaskDomain
import com.lifetracker.mobile.domain.repository.TaskRepository

class CreateTaskUseCase(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(params: CreateTaskParams): DomainResult<GameTaskDomain> =
        taskRepository.createTask(params)
}