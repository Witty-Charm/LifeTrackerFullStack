package com.lifetracker.mobile.domain.usecase

import com.lifetracker.mobile.domain.model.DomainResult
import com.lifetracker.mobile.domain.model.GameTaskDomain
import com.lifetracker.mobile.domain.repository.TaskRepository

class GetTaskUseCase(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(id: Int): DomainResult<GameTaskDomain> =
        repository.getTask(id)
}