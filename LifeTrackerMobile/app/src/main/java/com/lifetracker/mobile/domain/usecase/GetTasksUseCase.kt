package com.lifetracker.mobile.domain.usecase

import com.lifetracker.mobile.domain.model.DomainResult
import com.lifetracker.mobile.domain.model.GameTaskDomain
import com.lifetracker.mobile.domain.repository.TaskRepository

class GetTasksUseCase(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(heroId: Int): DomainResult<List<GameTaskDomain>> =
        taskRepository.getTasks(heroId)
}