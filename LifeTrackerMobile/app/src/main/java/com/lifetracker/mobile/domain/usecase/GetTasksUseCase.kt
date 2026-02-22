package com.lifetracker.mobile.domain.usecase

import com.lifetracker.mobile.domain.model.DomainResult
import com.lifetracker.mobile.domain.model.GameTaskDomain
import com.lifetracker.mobile.domain.repository.TaskRepository

class GetTasksUseCase(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(heroId: Int): DomainResult<List<GameTaskDomain>> =
        repository.getTasks(heroId)
}