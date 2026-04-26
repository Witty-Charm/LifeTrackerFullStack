package com.lifetracker.mobile.data.repository

import com.lifetracker.mobile.core.network.SafeApiCaller
import com.lifetracker.mobile.core.network.map
import com.lifetracker.mobile.core.sync.SyncScheduler
import com.lifetracker.mobile.data.local.dao.TaskDao
import com.lifetracker.mobile.data.mapper.toDomain
import com.lifetracker.mobile.data.mapper.toDomainResult
import com.lifetracker.mobile.data.mapper.toDto
import com.lifetracker.mobile.data.mapper.toEntity
import com.lifetracker.mobile.data.remote.LifeTrackerApi
import com.lifetracker.mobile.data.remote.dto.CreateTaskRequest
import com.lifetracker.mobile.data.remote.dto.SetDailyTaskStateRequest
import com.lifetracker.mobile.data.remote.dto.UpdateTaskRequest
import com.lifetracker.mobile.domain.model.CreateTaskParams
import com.lifetracker.mobile.domain.model.DomainResult
import com.lifetracker.mobile.domain.model.GameTaskDomain
import com.lifetracker.mobile.domain.model.HabitPolarity
import com.lifetracker.mobile.domain.model.OverdueResult
import com.lifetracker.mobile.domain.model.TaskCompletionResult
import com.lifetracker.mobile.domain.model.TaskFailureResult
import com.lifetracker.mobile.domain.model.TaskType
import com.lifetracker.mobile.domain.model.UpdateTaskParams
import com.lifetracker.mobile.domain.repository.TaskRepository
import timber.log.Timber

class TaskRepositoryImpl(
    private val api: LifeTrackerApi,
    private val caller: SafeApiCaller,
    private val taskDao: TaskDao,
    private val syncScheduler: SyncScheduler,
) : TaskRepository {
    override suspend fun getTasks(heroId: Int): DomainResult<List<GameTaskDomain>> {
        val local = taskDao.getByHeroId(heroId).map { it.toDomain() }

        val remote =
            caller
                .safeApiCall { api.getTasks(heroId) }
                .map { list -> list.map { it.toDomain() } }
                .toDomainResult()

        return when (remote) {
            is DomainResult.Success -> {
                taskDao.upsertAll(remote.data.map { it.toEntity() })
                remote
            }

            is DomainResult.Failure -> {
                if (local.isNotEmpty()) {
                    Timber.w("getTasks: network failed, returning ${local.size} cached tasks")
                    DomainResult.Success(local)
                } else {
                    remote
                }
            }
        }
    }

    override suspend fun getTask(id: Int): DomainResult<GameTaskDomain> {
        val remote =
            caller
                .safeApiCall { api.getTask(id) }
                .map { it.toDomain() }
                .toDomainResult()

        return when (remote) {
            is DomainResult.Success -> {
                taskDao.upsert(remote.data.toEntity())
                remote
            }

            is DomainResult.Failure -> {
                val local = taskDao.getById(id)?.toDomain()
                if (local != null) DomainResult.Success(local) else remote
            }
        }
    }

    override suspend fun createTask(params: CreateTaskParams): DomainResult<GameTaskDomain> {
        val remote =
            caller
                .safeApiCall {
                    api.createTask(
                        CreateTaskRequest(
                            heroId = params.heroId,
                            title = params.title,
                            description = params.description,
                            type = params.type.toDto(),
                            difficulty = params.difficulty.toDto(),
                            habitPolarity = params.habitPolarity.toRequestPolarity(params.type),
                            dueDate = params.dueDate,
                            repeatPattern = params.repeatPattern,
                            initialStreak = params.initialStreak,
                            checklistJson = params.checklistJson,
                            remindersJson = params.remindersJson,
                        ),
                    )
                }.map { it.toDomain() }
                .toDomainResult()

        return when (remote) {
            is DomainResult.Success -> {
                taskDao.upsert(remote.data.toEntity(pendingSync = false))
                remote
            }

            is DomainResult.Failure -> {
                val placeholder =
                    GameTaskDomain(
                        id = 0,
                        heroId = params.heroId,
                        title = params.title,
                        description = params.description ?: "",
                        type = params.type,
                        difficulty = params.difficulty,
                        habitPolarity = if (params.type == TaskType.Habit) params.habitPolarity else HabitPolarity.Both,
                        isCompleted = false,
                        isActive = true,
                        dueDate = params.dueDate,
                        repeatPattern = params.repeatPattern,
                        checklistJson = params.checklistJson,
                        remindersJson = params.remindersJson,
                        isOverdue = false,
                        completionCount = 0,
                        failCount = 0,
                        lastCompletedAt = null,
                        overdueProcessedAt = null,
                        baseXp = 0,
                        baseGold = 0,
                        hpPenalty = 0,
                        goldPenalty = 0,
                        streak = null,
                    )
                val savedEntity = taskDao.insertOfflineTask(placeholder.toEntity(pendingSync = true))
                syncScheduler.schedule()
                DomainResult.Success(savedEntity.toDomain())
            }
        }
    }

    override suspend fun updateTask(params: UpdateTaskParams): DomainResult<GameTaskDomain> {
        val remote =
            caller
                .safeApiCall {
                    api.updateTask(
                        params.taskId,
                        UpdateTaskRequest(
                            title = params.title,
                            description = params.description,
                            difficulty = params.difficulty.toDto(),
                            habitPolarity = params.habitPolarity.toRequestPolarity(params.type),
                            dueDate = params.dueDate,
                            repeatPattern = params.repeatPattern,
                            checklistJson = params.checklistJson,
                            remindersJson = params.remindersJson,
                        ),
                    )
                }.map { it.toDomain() }
                .toDomainResult()

        if (remote is DomainResult.Success) {
            taskDao.upsert(remote.data.toEntity(pendingSync = false))
        }

        return remote
    }

    override suspend fun completeTask(taskId: Int): DomainResult<TaskCompletionResult> {
        val remote =
            caller
                .safeApiCall { api.completeTask(taskId) }
                .map { it.toDomain() }
                .toDomainResult()

        if (remote is DomainResult.Success) {
            taskDao.getById(taskId)?.let { entity ->
                taskDao.upsert(entity.copy(isCompleted = true, pendingSync = false))
            }
        }

        return remote
    }

    override suspend fun setDailyTaskState(
        taskId: Int,
        localDate: String,
        isChecked: Boolean,
    ): DomainResult<TaskCompletionResult> {
        val remote =
            caller
                .safeApiCall {
                    api.setDailyTaskState(
                        taskId,
                        SetDailyTaskStateRequest(
                            localDate = localDate,
                            isChecked = isChecked,
                        ),
                    )
                }.map { it.toDomain() }
                .toDomainResult()

        if (remote is DomainResult.Success) {
            taskDao.getById(taskId)?.let { entity ->
                taskDao.upsert(
                    entity.copy(
                        isCompleted = entity.isCompleted,
                        pendingSync = false,
                    ),
                )
            }
        }

        return remote
    }

    override suspend fun failTask(taskId: Int): DomainResult<TaskFailureResult> {
        val remote =
            caller
                .safeApiCall { api.failTask(taskId) }
                .map { it.toDomain() }
                .toDomainResult()

        if (remote is DomainResult.Success) {
            taskDao.getById(taskId)?.let { entity ->
                taskDao.upsert(entity.copy(failCount = entity.failCount + 1, pendingSync = false))
            }
        }

        return remote
    }

    override suspend fun checkOverdueTasks(heroId: Int): DomainResult<OverdueResult> =
        caller
            .safeApiCall { api.checkOverdueTasks(heroId) }
            .map { it.toDomain() }
            .toDomainResult()

    override suspend fun deleteTask(taskId: Int): DomainResult<Unit> {
        if (taskId < 0) {
            taskDao.deleteById(taskId)
            return DomainResult.Success(Unit)
        }

        val remote =
            caller
                .safeApiCallUnit { api.deleteTask(taskId) }
                .toDomainResult()

        if (remote is DomainResult.Success) {
            taskDao.deleteById(taskId)
        }
        return remote
    }

    override suspend fun retryTaskSync(taskId: Int): DomainResult<Unit> {
        taskDao.getById(taskId)?.let {
            taskDao.upsert(it.copy(pendingSync = true, syncError = null))
            syncScheduler.schedule()
        }
        return DomainResult.Success(Unit)
    }

    override suspend fun deleteLocalTask(taskId: Int): DomainResult<Unit> {
        taskDao.deleteById(taskId)
        return DomainResult.Success(Unit)
    }
}

private fun HabitPolarity.toRequestPolarity(taskType: TaskType): com.lifetracker.mobile.data.remote.dto.HabitPolarity? =
    if (taskType == TaskType.Habit) toDto() else null
