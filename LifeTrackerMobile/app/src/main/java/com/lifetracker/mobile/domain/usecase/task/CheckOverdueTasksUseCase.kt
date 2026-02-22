package com.lifetracker.mobile.domain.usecase.task

import com.lifetracker.mobile.domain.model.DomainResult
import com.lifetracker.mobile.domain.model.OverdueResult
import com.lifetracker.mobile.domain.repository.TaskRepository

class CheckOverdueTasksUseCase(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(heroId: Int): DomainResult<OverdueResult> =
        repository.checkOverdueTasks(heroId)
}