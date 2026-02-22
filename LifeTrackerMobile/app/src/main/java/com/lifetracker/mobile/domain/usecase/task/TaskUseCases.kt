package com.lifetracker.mobile.domain.usecase.task

data class TaskUseCases(
    val getTasks: GetTasksUseCase,
    val getTask: GetTaskUseCase,
    val createTask: CreateTaskUseCase,
    val completeTask: CompleteTaskUseCase,
    val failTask: FailTaskUseCase,
    val checkOverdue: CheckOverdueTasksUseCase,
    val deleteTask: DeleteTaskUseCase,
)
