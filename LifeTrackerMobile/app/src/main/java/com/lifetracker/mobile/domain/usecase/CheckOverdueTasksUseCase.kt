package com.lifetracker.mobile.domain.usecase

import com.lifetracker.mobile.domain.model.DomainResult
import com.lifetracker.mobile.domain.model.OverdueResult
import com.lifetracker.mobile.domain.repository.TaskRepository

class CheckOverdueTasksUseCase(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(heroId: Int): DomainResult<OverdueResult> =
        taskRepository.checkOverdueTasks(heroId)
}