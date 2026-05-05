package com.lifetracker.mobile.ui.viewmodel

import androidx.work.WorkManager
import com.lifetracker.mobile.domain.auth.AuthRepository
import com.lifetracker.mobile.domain.auth.AuthSessionState
import com.lifetracker.mobile.domain.model.AchievementDomain
import com.lifetracker.mobile.domain.model.CreateTaskParams
import com.lifetracker.mobile.domain.model.UpdateTaskParams
import com.lifetracker.mobile.domain.model.DomainResult
import com.lifetracker.mobile.domain.model.GameError
import com.lifetracker.mobile.domain.model.GameTaskDomain
import com.lifetracker.mobile.domain.model.HabitPolarity
import com.lifetracker.mobile.domain.model.HealResult
import com.lifetracker.mobile.domain.model.HeroDomain
import com.lifetracker.mobile.domain.model.HeroStatsDomain
import com.lifetracker.mobile.domain.model.OverdueResult
import com.lifetracker.mobile.domain.model.RespawnResult
import com.lifetracker.mobile.domain.model.TaskCompletionResult
import com.lifetracker.mobile.domain.model.TaskDifficulty
import com.lifetracker.mobile.domain.model.TaskFailureResult
import com.lifetracker.mobile.domain.model.TaskType
import com.lifetracker.mobile.domain.repository.HeroRepository
import com.lifetracker.mobile.domain.repository.TaskRepository
import com.lifetracker.mobile.domain.usecase.hero.CreateHeroUseCase
import com.lifetracker.mobile.domain.usecase.hero.GetCurrentHeroUseCase
import com.lifetracker.mobile.domain.usecase.hero.GetFirstHeroUseCase
import com.lifetracker.mobile.domain.usecase.hero.GetHeroAchievementsUseCase
import com.lifetracker.mobile.domain.usecase.hero.GetHeroStatsUseCase
import com.lifetracker.mobile.domain.usecase.hero.GetHeroUseCase
import com.lifetracker.mobile.domain.usecase.hero.GetHeroesUseCase
import com.lifetracker.mobile.domain.usecase.hero.HealHeroUseCase
import com.lifetracker.mobile.domain.usecase.hero.HeroUseCases
import com.lifetracker.mobile.domain.usecase.hero.RespawnHeroUseCase
import com.lifetracker.mobile.domain.usecase.hero.UpdateHeroTimeZoneUseCase
import com.lifetracker.mobile.domain.usecase.task.CheckOverdueTasksUseCase
import com.lifetracker.mobile.domain.usecase.task.CompleteTaskUseCase
import com.lifetracker.mobile.domain.usecase.task.CreateTaskUseCase
import com.lifetracker.mobile.domain.usecase.task.UpdateTaskUseCase
import com.lifetracker.mobile.domain.usecase.task.DeleteLocalTaskUseCase
import com.lifetracker.mobile.domain.usecase.task.DeleteTaskUseCase
import com.lifetracker.mobile.domain.usecase.task.FailTaskUseCase
import com.lifetracker.mobile.domain.usecase.task.GetTaskUseCase
import com.lifetracker.mobile.domain.usecase.task.GetTasksUseCase
import com.lifetracker.mobile.domain.usecase.task.RetryTaskSyncUseCase
import com.lifetracker.mobile.domain.usecase.task.SetDailyTaskStateUseCase
import com.lifetracker.mobile.domain.usecase.task.TaskUseCases
import com.lifetracker.mobile.ui.model.UiEvent
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class HeroViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun refreshOnForeground_reloadsData_whenOutsideDebounceWindow() =
        runTest {
            val heroRepository = FakeHeroRepository()
            val taskRepository = FakeTaskRepository()
            val viewModel = buildViewModel(heroRepository, taskRepository)

            advanceUntilIdle()
            assertEquals(1, heroRepository.getCurrentHeroCalls)
            assertEquals(0, heroRepository.getFirstHeroCalls)
            assertEquals(1, taskRepository.getTasksCalls)
            assertEquals(1, taskRepository.checkOverdueCalls)

            viewModel.refreshOnForeground(nowMillis = 31_000L)
            advanceUntilIdle()

            assertEquals(2, heroRepository.getCurrentHeroCalls)
            assertEquals(0, heroRepository.getFirstHeroCalls)
            assertEquals(2, taskRepository.getTasksCalls)
            assertEquals(2, taskRepository.checkOverdueCalls)
        }

    @Test
    fun refreshOnForeground_debouncesRepeatedCalls_untilIntervalPasses() =
        runTest {
            val heroRepository = FakeHeroRepository()
            val taskRepository = FakeTaskRepository()
            val viewModel = buildViewModel(heroRepository, taskRepository)

            advanceUntilIdle()

            viewModel.refreshOnForeground(nowMillis = 31_000L)
            advanceUntilIdle()
            viewModel.refreshOnForeground(nowMillis = 40_000L)
            advanceUntilIdle()

            assertEquals(2, heroRepository.getCurrentHeroCalls)
            assertEquals(0, heroRepository.getFirstHeroCalls)
            assertEquals(2, taskRepository.getTasksCalls)

            viewModel.refreshOnForeground(nowMillis = 62_000L)
            advanceUntilIdle()

            assertEquals(3, heroRepository.getCurrentHeroCalls)
            assertEquals(0, heroRepository.getFirstHeroCalls)
            assertEquals(3, taskRepository.getTasksCalls)
        }

    @Test
    fun refreshOnForeground_skipsReload_whenInitialLoadIsStillRunning() =
        runTest {
            val heroRepository = FakeHeroRepository()
            val taskRepository = FakeTaskRepository()
            val viewModel = buildViewModel(heroRepository, taskRepository)

            viewModel.refreshOnForeground(nowMillis = 31_000L)
            advanceUntilIdle()

            assertEquals(1, heroRepository.getCurrentHeroCalls)
            assertEquals(0, heroRepository.getFirstHeroCalls)
            assertEquals(1, taskRepository.getTasksCalls)
            assertEquals(1, taskRepository.checkOverdueCalls)
        }

    @Test
    fun completeTask_sendsAchievementSnackbar_whenResponseContainsUnlockedAchievements() =
        runTest {
            val heroRepository = FakeHeroRepository()
            val taskRepository =
                FakeTaskRepository().apply {
                    completeTaskResult =
                        DomainResult.Success(
                            TaskCompletionResult(
                                taskId = 1,
                                taskTitle = "Task",
                                xpGained = 10,
                                goldGained = 8,
                                leveledUp = false,
                                newLevel = 3,
                                streakBonus = 0,
                                currentStreak = 0,
                                message = "Task completed!",
                                heroSnapshot = createHeroSnapshot(gold = 33),
                                unlockedAchievements =
                                    listOf(
                                        AchievementDomain(
                                            key = "tasks_10",
                                            title = "Task Starter",
                                            description = "Complete 10 tasks.",
                                            category = "TasksCompleted",
                                            threshold = 10,
                                            sortOrder = 10,
                                            goldReward = 25,
                                            unlocked = true,
                                            unlockedAt = Instant.parse("2026-04-20T10:00:00Z"),
                                        ),
                                    ),
                            ),
                        )
                }
            val viewModel = buildViewModel(heroRepository, taskRepository)
            advanceUntilIdle()

            val eventsDeferred = async { viewModel.events.take(2).toList() }

            viewModel.completeTask(1)
            advanceUntilIdle()

            val events = eventsDeferred.await()
            assertEquals(2, events.size)
            assertEquals(
                UiEvent.ShowSnackbar("Achievement unlocked: Task Starter (+25 Gold)"),
                events[1],
            )
        }

    @Test
    fun createTask_passesHabitPolarityToUseCase() =
        runTest {
            val heroRepository = FakeHeroRepository()
            val taskRepository = FakeTaskRepository()
            val viewModel = buildViewModel(heroRepository, taskRepository)
            advanceUntilIdle()

            viewModel.createTask(
                title = "Cold shower",
                description = null,
                type = com.lifetracker.mobile.ui.model.UiTaskType.Habit,
                difficulty = com.lifetracker.mobile.ui.model.UiDifficulty.Medium,
                dueDate = null,
                habitPolarity = HabitPolarity.Negative,
            )
            advanceUntilIdle()

            assertEquals(HabitPolarity.Negative, taskRepository.lastCreateTaskParams?.habitPolarity)
        }

    @Test
    fun completeTask_doesNotCallRepository_forLocalOnlyTask() =
        runTest {
            val heroRepository = FakeHeroRepository()
            val taskRepository =
                FakeTaskRepository().apply {
                    tasks = listOf(createTestTask(id = -3, syncError = "Server rejected"))
                }
            val viewModel = buildViewModel(heroRepository, taskRepository)
            advanceUntilIdle()

            viewModel.completeTask(-3)
            advanceUntilIdle()

            assertEquals(emptyList<Int>(), taskRepository.completeTaskCalls)
        }

    @Test
    fun deleteTask_doesNotStart_whenSameTaskAlreadyHasPendingMutation() =
        runTest {
            val heroRepository = FakeHeroRepository()
            val gate = CompletableDeferred<Unit>()
            val taskRepository =
                FakeTaskRepository().apply {
                    completeTaskGates[1] = gate
                    completeTaskResults[1] = DomainResult.Success(createCompletionResult(taskId = 1))
                }
            val viewModel = buildViewModel(heroRepository, taskRepository)
            advanceUntilIdle()

            viewModel.completeTask(1)
            advanceUntilIdle()
            viewModel.deleteTask(1)
            advanceUntilIdle()

            assertEquals(listOf(1), taskRepository.completeTaskCalls)
            assertEquals(emptyList<Int>(), taskRepository.deleteTaskCalls)

            gate.complete(Unit)
            advanceUntilIdle()
        }

    @Test
    fun completeTask_coalescesTaskRefresh_afterConsecutiveSuccesses() =
        runTest {
            val heroRepository = FakeHeroRepository()
            val taskRepository =
                FakeTaskRepository().apply {
                    tasks = listOf(createTestTask(id = 1), createTestTask(id = 2))
                    completeTaskResults[1] = DomainResult.Success(createCompletionResult(taskId = 1))
                    completeTaskResults[2] = DomainResult.Success(createCompletionResult(taskId = 2))
                }
            val viewModel = buildViewModel(heroRepository, taskRepository)
            advanceUntilIdle()

            viewModel.completeTask(1)
            viewModel.completeTask(2)
            advanceUntilIdle()

            assertEquals(listOf(1, 2), taskRepository.completeTaskCalls)
            assertEquals(2, taskRepository.getTasksCalls)
        }

    @Test
    fun deleteTask_softHidesTaskAndEmitsUndoPrompt_withoutCallingRepository() =
        runTest {
            val heroRepository = FakeHeroRepository()
            val taskRepository =
                FakeTaskRepository().apply {
                    tasks = listOf(createTestTask(id = 5, type = TaskType.Habit))
                }
            val viewModel = buildViewModel(heroRepository, taskRepository)
            advanceUntilIdle()

            val eventsDeferred = async { viewModel.events.take(1).toList() }

            viewModel.deleteTask(5)
            advanceUntilIdle()

            assertEquals(emptyList<Int>(), taskRepository.deleteTaskCalls)
            assertEquals(setOf(5), viewModel.state.value.pendingDeletionTaskIds.toSet())

            val events = eventsDeferred.await()
            assertEquals(1, events.size)
            val prompt = events.first() as UiEvent.UndoDeletePrompt
            assertEquals(5, prompt.taskId)
            assertEquals(com.lifetracker.mobile.ui.model.UiTaskType.Habit, prompt.taskType)
        }

    @Test
    fun undoDeleteTask_restoresVisibility_andRepositoryNeverCalled() =
        runTest {
            val heroRepository = FakeHeroRepository()
            val taskRepository =
                FakeTaskRepository().apply { tasks = listOf(createTestTask(id = 7)) }
            val viewModel = buildViewModel(heroRepository, taskRepository)
            advanceUntilIdle()

            viewModel.deleteTask(7)
            advanceUntilIdle()
            viewModel.undoDeleteTask(7)
            advanceUntilIdle()

            assertEquals(emptyList<Int>(), taskRepository.deleteTaskCalls)
            assertEquals(emptySet<Int>(), viewModel.state.value.pendingDeletionTaskIds.toSet())
            assertEquals(1, viewModel.state.value.tasks.size)
        }

    @Test
    fun confirmDeleteTask_callsRepository_andRemovesTaskOnSuccess() =
        runTest {
            val heroRepository = FakeHeroRepository()
            val taskRepository =
                FakeTaskRepository().apply { tasks = listOf(createTestTask(id = 9)) }
            val viewModel = buildViewModel(heroRepository, taskRepository)
            advanceUntilIdle()

            viewModel.deleteTask(9)
            advanceUntilIdle()
            viewModel.confirmDeleteTask(9)
            advanceUntilIdle()

            assertEquals(listOf(9), taskRepository.deleteTaskCalls)
            assertEquals(emptySet<Int>(), viewModel.state.value.pendingDeletionTaskIds.toSet())
            assertEquals(emptyList<Int>(), viewModel.state.value.tasks.map { it.id })
        }

    @Test
    fun confirmDeleteTask_onFailure_restoresTaskWithActionError() =
        runTest {
            val heroRepository = FakeHeroRepository()
            val taskRepository =
                FakeTaskRepository().apply {
                    tasks = listOf(createTestTask(id = 11))
                    deleteTaskFailure = GameError.Unknown("Server failed")
                }
            val viewModel = buildViewModel(heroRepository, taskRepository)
            advanceUntilIdle()

            viewModel.deleteTask(11)
            advanceUntilIdle()
            viewModel.confirmDeleteTask(11)
            advanceUntilIdle()

            assertEquals(listOf(11), taskRepository.deleteTaskCalls)
            assertEquals(emptySet<Int>(), viewModel.state.value.pendingDeletionTaskIds.toSet())
            val task = viewModel.state.value.tasks.first { it.id == 11 }
            assertEquals("Action failed. Please try again.", task.actionError)
        }

    @Test
    fun completeTask_dailyUsesExplicitDailyStateFlowAndKeepsTaskInList() =
        runTest {
            val heroRepository = FakeHeroRepository()
            val taskRepository =
                FakeTaskRepository().apply {
                    tasks = listOf(createTestTask(id = 1, type = TaskType.Daily))
                    setDailyTaskStateResults[1] = DomainResult.Success(createDailyStateResult(taskId = 1, isChecked = true))
                }
            val viewModel = buildViewModel(heroRepository, taskRepository)
            advanceUntilIdle()

            viewModel.completeTask(1)
            advanceUntilIdle()

            val expectedLocalDate = Clock.System.now().toLocalDateTime(TimeZone.of(heroRepository.hero.timeZoneId)).date.toString()

            assertEquals(emptyList<Int>(), taskRepository.completeTaskCalls)
            assertEquals(listOf(Triple(1, expectedLocalDate, true)), taskRepository.setDailyTaskStateCalls)
            assertEquals(1, viewModel.state.value.tasks.size)
        }

    private fun buildViewModel(
        heroRepository: FakeHeroRepository,
        taskRepository: FakeTaskRepository,
    ): HeroViewModel {
        val workManager = mockk<WorkManager>()
        every { workManager.getWorkInfosForUniqueWorkFlow(any()) } returns emptyFlow()

        return HeroViewModel(
            heroUseCases =
                HeroUseCases(
                    getHeroes = GetHeroesUseCase(heroRepository),
                    getHero = GetHeroUseCase(heroRepository),
                    getCurrentHero = GetCurrentHeroUseCase(heroRepository),
                    getFirstHero = GetFirstHeroUseCase(heroRepository),
                    createHero = CreateHeroUseCase(heroRepository),
                    getHeroStats = GetHeroStatsUseCase(heroRepository),
                    getHeroAchievements = GetHeroAchievementsUseCase(heroRepository),
                    respawnHero = RespawnHeroUseCase(heroRepository),
                    healHero = HealHeroUseCase(heroRepository),
                    updateHeroTimeZone = UpdateHeroTimeZoneUseCase(heroRepository),
                ),
            taskUseCases =
                TaskUseCases(
                    getTasks = GetTasksUseCase(taskRepository),
                    getTask = GetTaskUseCase(taskRepository),
                    createTask = CreateTaskUseCase(taskRepository),
                    updateTask = UpdateTaskUseCase(taskRepository),
                    completeTask = CompleteTaskUseCase(taskRepository),
                    setDailyTaskState = SetDailyTaskStateUseCase(taskRepository),
                    failTask = FailTaskUseCase(taskRepository),
                    checkOverdue = CheckOverdueTasksUseCase(taskRepository),
                    deleteTask = DeleteTaskUseCase(taskRepository),
                    retryTaskSync = RetryTaskSyncUseCase(taskRepository),
                    deleteLocalTask = DeleteLocalTaskUseCase(taskRepository),
                ),
            workManager = workManager,
            auth = NoOpAuthRepository(),
        )
    }

    private class NoOpAuthRepository : AuthRepository {
        override val authStateFlow: StateFlow<AuthSessionState> =
            MutableStateFlow(AuthSessionState.SignedIn(userId = 0, email = "", displayName = ""))

        override suspend fun signInWithGoogle(idToken: String): Result<AuthSessionState.SignedIn> =
            Result.failure(UnsupportedOperationException("not used in tests"))

        override suspend fun signOut() = Unit
    }

    private class FakeHeroRepository : HeroRepository {
        var achievementsResult: DomainResult<List<AchievementDomain>> = DomainResult.Success(emptyList())
        var getCurrentHeroCalls: Int = 0
        var getFirstHeroCalls: Int = 0
        val hero = createTestHero()

        override suspend fun getHeroes(): DomainResult<List<HeroDomain>> = DomainResult.Success(listOf(hero))

        override suspend fun getHero(id: Int): DomainResult<HeroDomain> = DomainResult.Success(hero)

        override suspend fun getCurrentHero(): DomainResult<HeroDomain?> {
            getCurrentHeroCalls++
            return DomainResult.Success(hero)
        }

        override suspend fun getFirstHero(): DomainResult<HeroDomain?> {
            getFirstHeroCalls++
            return DomainResult.Success(hero)
        }

        override suspend fun createHero(
            name: String,
            startingGold: Int?,
        ): DomainResult<HeroDomain> = DomainResult.Success(hero.copy(name = name, gold = startingGold ?: hero.gold))

        override suspend fun getHeroStats(heroId: Int): DomainResult<HeroStatsDomain> =
            DomainResult.Failure(GameError.Unknown("Not used in this test"))

        override suspend fun getHeroAchievements(heroId: Int): DomainResult<List<AchievementDomain>> = achievementsResult

        override suspend fun respawnHero(heroId: Int): DomainResult<RespawnResult> =
            DomainResult.Failure(GameError.Unknown("Not used in this test"))

        override suspend fun healHero(
            heroId: Int,
            amount: Int?,
        ): DomainResult<HealResult> = DomainResult.Failure(GameError.Unknown("Not used in this test"))

        override suspend fun updateHeroTimeZone(
            heroId: Int,
            timeZoneId: String,
        ): DomainResult<Unit> = DomainResult.Success(Unit)
    }

    private class FakeTaskRepository : TaskRepository {
        var getTasksCalls: Int = 0
        var checkOverdueCalls: Int = 0
        var completeTaskResult: DomainResult<TaskCompletionResult> = DomainResult.Failure(GameError.Unknown("Not used in this test"))
        var lastCreateTaskParams: CreateTaskParams? = null
        var tasks: List<GameTaskDomain> = listOf(createTestTask())
        val completeTaskCalls = mutableListOf<Int>()
        val setDailyTaskStateCalls = mutableListOf<Triple<Int, String, Boolean>>()
        val deleteTaskCalls = mutableListOf<Int>()
        var deleteTaskFailure: GameError? = null
        val completeTaskResults = mutableMapOf<Int, DomainResult<TaskCompletionResult>>()
        val setDailyTaskStateResults = mutableMapOf<Int, DomainResult<TaskCompletionResult>>()
        val completeTaskGates = mutableMapOf<Int, CompletableDeferred<Unit>>()

        override suspend fun getTasks(heroId: Int): DomainResult<List<GameTaskDomain>> {
            getTasksCalls++
            return DomainResult.Success(tasks)
        }

        override suspend fun getTask(id: Int): DomainResult<GameTaskDomain> = DomainResult.Success(tasks.first { it.id == id })

        override suspend fun createTask(params: CreateTaskParams): DomainResult<GameTaskDomain> {
            lastCreateTaskParams = params
            return DomainResult.Success(createTestTask(type = params.type, habitPolarity = params.habitPolarity))
        }

        override suspend fun updateTask(params: UpdateTaskParams): DomainResult<GameTaskDomain> =
            DomainResult.Success(createTestTask(type = params.type, habitPolarity = params.habitPolarity))

        override suspend fun completeTask(taskId: Int): DomainResult<TaskCompletionResult> {
            completeTaskCalls += taskId
            completeTaskGates[taskId]?.await()
            return completeTaskResults[taskId] ?: completeTaskResult
        }

        override suspend fun setDailyTaskState(
            taskId: Int,
            localDate: String,
            isChecked: Boolean,
        ): DomainResult<TaskCompletionResult> {
            setDailyTaskStateCalls += Triple(taskId, localDate, isChecked)
            return setDailyTaskStateResults[taskId] ?: DomainResult.Failure(GameError.Unknown("Not used in this test"))
        }

        override suspend fun failTask(taskId: Int): DomainResult<TaskFailureResult> =
            DomainResult.Failure(GameError.Unknown("Not used in this test"))

        override suspend fun checkOverdueTasks(heroId: Int): DomainResult<OverdueResult> {
            checkOverdueCalls++
            return DomainResult.Success(OverdueResult(overdueCount = 0, penalties = emptyList(), message = ""))
        }

        override suspend fun deleteTask(taskId: Int): DomainResult<Unit> {
            deleteTaskCalls += taskId
            return deleteTaskFailure?.let { DomainResult.Failure(it) } ?: run {
                tasks = tasks.filterNot { it.id == taskId }
                DomainResult.Success(Unit)
            }
        }

        override suspend fun retryTaskSync(taskId: Int): DomainResult<Unit> = DomainResult.Success(Unit)

        override suspend fun deleteLocalTask(taskId: Int): DomainResult<Unit> = DomainResult.Success(Unit)
    }

    companion object {
        private fun createTestHero() =
            HeroDomain(
                id = 1,
                name = "Alex",
                timeZoneId = "UTC",
                level = 3,
                currentXp = 40,
                maxXp = 100,
                currentHp = 90,
                maxHp = 100,
                gold = 50,
                isDead = false,
                deathCount = 0,
                isInRecovery = false,
                recoveryMultiplier = 1.0,
                xpBoostPercent = 0,
                xpBoostTasksRemaining = 0,
                dailyCompletions = 1,
                dailyCompletionsMax = 5,
            )

        private fun createHeroSnapshot(gold: Int) =
            com.lifetracker.mobile.domain.model.HeroSnapshot(
                heroId = 1,
                level = 3,
                currentXp = 40,
                xpForNextLevel = 100,
                currentHp = 90,
                maxHp = 100,
                gold = gold,
                deathCount = 0,
                dailyCompletions = 1,
                dailyCompletionsMax = 5,
                isDead = false,
                xpBoostPercent = 0,
                xpBoostTasksRemaining = 0,
            )

        private fun createCompletionResult(taskId: Int) =
            TaskCompletionResult(
                taskId = taskId,
                taskTitle = "Task $taskId",
                xpGained = 10,
                goldGained = 5,
                leveledUp = false,
                newLevel = 3,
                streakBonus = 0,
                currentStreak = 0,
                message = "Completed",
                heroSnapshot = createHeroSnapshot(gold = 55),
                unlockedAchievements = emptyList(),
            )

        private fun createDailyStateResult(
            taskId: Int,
            isChecked: Boolean,
        ) =
            TaskCompletionResult(
                taskId = taskId,
                taskTitle = "Task $taskId",
                xpGained = if (isChecked) 10 else -10,
                goldGained = if (isChecked) 5 else -5,
                leveledUp = false,
                newLevel = 3,
                streakBonus = 0,
                currentStreak = if (isChecked) 1 else 0,
                message = if (isChecked) "Daily checked" else "Daily unchecked",
                heroSnapshot = createHeroSnapshot(gold = if (isChecked) 55 else 45),
                unlockedAchievements = emptyList(),
            )

        private fun createTestTask(
            id: Int = 1,
            type: TaskType = TaskType.OneTime,
            habitPolarity: HabitPolarity = HabitPolarity.Both,
            pendingSync: Boolean = false,
            syncError: String? = null,
        ) = GameTaskDomain(
            id = id,
            heroId = 1,
            title = "Task",
            description = "",
            type = type,
            difficulty = TaskDifficulty.Easy,
            habitPolarity = habitPolarity,
            isCompleted = false,
            isCheckedToday = false,
            isActive = true,
            dueDate = null,
            repeatPattern = null,
            checklistJson = null,
            remindersJson = null,
            isOverdue = false,
            completionCount = 0,
            failCount = 0,
            lastCompletedAt = null,
            overdueProcessedAt = null,
            baseXp = 10,
            baseGold = 5,
            hpPenalty = 0,
            goldPenalty = 0,
            streak = null,
            pendingSync = pendingSync,
            syncError = syncError,
        )
    }
}
