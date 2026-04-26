package com.lifetracker.mobile.domain.usecase.task

data class TaskUseCases(
    val getTasks: GetTasksUseCase,
    val getTask: GetTaskUseCase,
    val createTask: CreateTaskUseCase,
    val updateTask: UpdateTaskUseCase,
    val completeTask: CompleteTaskUseCase,
    val setDailyTaskState: SetDailyTaskStateUseCase,
    val failTask: FailTaskUseCase,
    val checkOverdue: CheckOverdueTasksUseCase,
    val deleteTask: DeleteTaskUseCase,
    val retryTaskSync: RetryTaskSyncUseCase,
    val deleteLocalTask: DeleteLocalTaskUseCase,
)
