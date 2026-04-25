package com.lifetracker.mobile.domain.usecase.task

import com.lifetracker.mobile.domain.model.DomainResult
import com.lifetracker.mobile.domain.model.TaskCompletionResult
import com.lifetracker.mobile.domain.repository.TaskRepository

class SetDailyTaskStateUseCase(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(
        taskId: Int,
        localDate: String,
        isChecked: Boolean,
    ): DomainResult<TaskCompletionResult> =
        repository.setDailyTaskState(taskId, localDate, isChecked)
}
