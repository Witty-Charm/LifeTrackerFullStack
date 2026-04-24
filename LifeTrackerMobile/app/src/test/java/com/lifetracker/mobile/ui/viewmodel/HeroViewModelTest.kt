package com.lifetracker.mobile.ui.viewmodel

import androidx.work.WorkManager
import com.lifetracker.mobile.domain.model.AchievementDomain
import com.lifetracker.mobile.domain.model.CreateTaskParams
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
import com.lifetracker.mobile.domain.usecase.task.DeleteLocalTaskUseCase
import com.lifetracker.mobile.domain.usecase.task.DeleteTaskUseCase
import com.lifetracker.mobile.domain.usecase.task.FailTaskUseCase
import com.lifetracker.mobile.domain.usecase.task.GetTaskUseCase
import com.lifetracker.mobile.domain.usecase.task.GetTasksUseCase
import com.lifetracker.mobile.domain.usecase.task.RetryTaskSyncUseCase
import com.lifetracker.mobile.domain.usecase.task.TaskUseCases
import com.lifetracker.mobile.ui.model.UiEvent
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
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
                    completeTask = CompleteTaskUseCase(taskRepository),
                    failTask = FailTaskUseCase(taskRepository),
                    checkOverdue = CheckOverdueTasksUseCase(taskRepository),
                    deleteTask = DeleteTaskUseCase(taskRepository),
                    retryTaskSync = RetryTaskSyncUseCase(taskRepository),
                    deleteLocalTask = DeleteLocalTaskUseCase(taskRepository),
                ),
            workManager = workManager,
        )
    }

    private class FakeHeroRepository : HeroRepository {
        var achievementsResult: DomainResult<List<AchievementDomain>> = DomainResult.Success(emptyList())
        var getCurrentHeroCalls: Int = 0
        var getFirstHeroCalls: Int = 0
        private val hero = createTestHero()

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
        private val tasks = listOf(createTestTask())

        override suspend fun getTasks(heroId: Int): DomainResult<List<GameTaskDomain>> {
            getTasksCalls++
            return DomainResult.Success(tasks)
        }

        override suspend fun getTask(id: Int): DomainResult<GameTaskDomain> = DomainResult.Success(tasks.first())

        override suspend fun createTask(params: CreateTaskParams): DomainResult<GameTaskDomain> {
            lastCreateTaskParams = params
            return DomainResult.Success(createTestTask(type = params.type, habitPolarity = params.habitPolarity))
        }

        override suspend fun completeTask(taskId: Int): DomainResult<TaskCompletionResult> = completeTaskResult

        override suspend fun failTask(taskId: Int): DomainResult<TaskFailureResult> =
            DomainResult.Failure(GameError.Unknown("Not used in this test"))

        override suspend fun checkOverdueTasks(heroId: Int): DomainResult<OverdueResult> {
            checkOverdueCalls++
            return DomainResult.Success(OverdueResult(overdueCount = 0, penalties = emptyList(), message = ""))
        }

        override suspend fun deleteTask(taskId: Int): DomainResult<Unit> = DomainResult.Success(Unit)

        override suspend fun retryTaskSync(taskId: Int): DomainResult<Unit> = DomainResult.Success(Unit)

        override suspend fun deleteLocalTask(taskId: Int): DomainResult<Unit> = DomainResult.Success(Unit)
    }

    companion object {
        private fun createTestHero() =
            HeroDomain(
                id = 1,
                name = "Alex",
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

        private fun createTestTask(
            type: TaskType = TaskType.OneTime,
            habitPolarity: HabitPolarity = HabitPolarity.Both,
        ) = GameTaskDomain(
            id = 1,
            heroId = 1,
            title = "Task",
            description = "",
            type = type,
            difficulty = TaskDifficulty.Easy,
            habitPolarity = habitPolarity,
            isCompleted = false,
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
            pendingSync = false,
            syncError = null,
        )
    }
}
