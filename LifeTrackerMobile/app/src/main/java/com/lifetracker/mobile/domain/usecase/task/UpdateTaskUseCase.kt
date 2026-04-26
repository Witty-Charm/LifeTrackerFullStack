package com.lifetracker.mobile.domain.usecase.task

import com.lifetracker.mobile.domain.model.DomainResult
import com.lifetracker.mobile.domain.model.GameError
import com.lifetracker.mobile.domain.model.GameTaskDomain
import com.lifetracker.mobile.domain.model.TaskDifficulty
import com.lifetracker.mobile.domain.model.UpdateTaskParams
import com.lifetracker.mobile.domain.repository.TaskRepository

class UpdateTaskUseCase(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(params: UpdateTaskParams): DomainResult<GameTaskDomain> {
        val errors = buildMap {
            if (params.title.isBlank())
                put("title", listOf("Title cannot be empty"))
            else if (params.title.length > 200)
                put("title", listOf("Title cannot exceed 200 characters"))

            if (params.difficulty == TaskDifficulty.Unknown)
                put("difficulty", listOf("Invalid difficulty"))
        }

        if (errors.isNotEmpty())
            return DomainResult.Failure(GameError.Validation(errors))

        return repository.updateTask(params)
    }
}
