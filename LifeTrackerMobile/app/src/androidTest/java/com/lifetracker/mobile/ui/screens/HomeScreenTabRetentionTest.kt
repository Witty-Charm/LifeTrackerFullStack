package com.lifetracker.mobile.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkManager
import com.lifetracker.mobile.domain.model.AchievementDomain
import com.lifetracker.mobile.domain.model.CreateTaskParams
import com.lifetracker.mobile.domain.model.DomainResult
import com.lifetracker.mobile.domain.model.GameError
import com.lifetracker.mobile.domain.model.GameTaskDomain
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
import com.lifetracker.mobile.ui.model.HeroScreenState
import com.lifetracker.mobile.ui.model.TaskUi
import com.lifetracker.mobile.ui.model.UiTaskType
import com.lifetracker.mobile.ui.theme.LifeTrackerMobileTheme
import com.lifetracker.mobile.ui.viewmodel.HeroViewModel
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTabRetentionTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun keeps_selected_tab_after_state_restoration() {
        val restorationTester = StateRestorationTester(composeRule)

        restorationTester.setContent {
            TestHomeScreen(homeState = testHomeState())
        }

        composeRule.onNodeWithContentDescription("Habits").performClick()
        assertHabitsSelected()

        restorationTester.emulateSavedInstanceStateRestore()

        assertHabitsSelected()
    }

    @Test
    fun keeps_selected_tab_when_home_state_updates_after_save() {
        var homeState by mutableStateOf(testHomeState())

        composeRule.setContent {
            TestHomeScreen(homeState = homeState)
        }

        composeRule.onNodeWithContentDescription("Habits").performClick()
        assertHabitsSelected()

        composeRule.runOnUiThread {
            homeState =
                homeState.copy(
                    tasks =
                        persistentListOf(
                            testTask(id = 1, title = "Habit task", type = UiTaskType.Habit),
                            testTask(id = 2, title = "Todo task", type = UiTaskType.OneTime),
                            testTask(id = 3, title = "Todo task 2", type = UiTaskType.OneTime),
                        ),
                )
        }

        composeRule.waitForIdle()

        assertHabitsSelected()
    }

    private fun assertHabitsSelected() {
        composeRule.onNodeWithText("Habit task").assertIsDisplayed()
        composeRule.onAllNodesWithText("Todo task").assertCountEquals(0)
        composeRule.onAllNodesWithText("Todo task 2").assertCountEquals(0)
    }
}

@Composable
private fun TestHomeScreen(homeState: HeroScreenState) {
    val navController = rememberNavController()

    LifeTrackerMobileTheme {
        HomeScreen(
            state = homeState,
            vm = rememberHeroViewModel(),
            navController = navController,
        )
    }
}

@Composable
private fun rememberHeroViewModel(): HeroViewModel {
    val context = LocalContext.current

    return remember {
        val heroRepository = FakeHeroRepository()
        val taskRepository = FakeTaskRepository()

        HeroViewModel(
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
                    getTask = GetTaskUseCase(taskRepository),
                    getTasks = GetTasksUseCase(taskRepository),
                    completeTask = CompleteTaskUseCase(taskRepository),
                    createTask = CreateTaskUseCase(taskRepository),
                    failTask = FailTaskUseCase(taskRepository),
                    deleteTask = DeleteTaskUseCase(taskRepository),
                    checkOverdue = CheckOverdueTasksUseCase(taskRepository),
                    retryTaskSync = RetryTaskSyncUseCase(taskRepository),
                    deleteLocalTask = DeleteLocalTaskUseCase(taskRepository),
                ),
            workManager = WorkManager.getInstance(context),
        )
    }
}

private fun testHomeState(): HeroScreenState =
    HeroScreenState(
        hero = null,
        tasks =
            persistentListOf(
                testTask(id = 1, title = "Habit task", type = UiTaskType.Habit),
                testTask(id = 2, title = "Todo task", type = UiTaskType.OneTime),
            ),
    )

private fun testTask(
    id: Int,
    title: String,
    type: UiTaskType,
): TaskUi =
    TaskUi(
        id = id,
        title = title,
        description = "",
        type = type,
        difficultyLabel = "Easy",
        difficultyColor = 0L,
        isCompleted = false,
        isOverdue = false,
        dueDateText = null,
        rewardText = "+10 XP • +5 Gold",
        penaltyText = "",
        streakText = null,
        isPendingSync = false,
    )

private class FakeHeroRepository : HeroRepository {
    private val hero =
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

    override suspend fun getHeroes(): DomainResult<List<HeroDomain>> = DomainResult.Success(listOf(hero))

    override suspend fun getHero(id: Int): DomainResult<HeroDomain> = DomainResult.Success(hero)

    override suspend fun getCurrentHero(): DomainResult<HeroDomain?> = DomainResult.Success(hero)

    override suspend fun getFirstHero(): DomainResult<HeroDomain?> = DomainResult.Success(hero)

    override suspend fun createHero(
        name: String,
        startingGold: Int?,
    ): DomainResult<HeroDomain> = DomainResult.Success(hero.copy(name = name, gold = startingGold ?: hero.gold))

    override suspend fun getHeroStats(heroId: Int): DomainResult<HeroStatsDomain> =
        DomainResult.Failure(GameError.Unknown("Not used in this test"))

    override suspend fun getHeroAchievements(heroId: Int): DomainResult<List<AchievementDomain>> = DomainResult.Success(emptyList())

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
    private val tasks =
        listOf(
            GameTaskDomain(
                id = 1,
                heroId = 1,
                title = "Todo task",
                description = "",
                type = TaskType.OneTime,
                difficulty = TaskDifficulty.Easy,
                habitPolarity = com.lifetracker.mobile.domain.model.HabitPolarity.Both,
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
            ),
        )

    override suspend fun getTasks(heroId: Int): DomainResult<List<GameTaskDomain>> = DomainResult.Success(tasks)

    override suspend fun getTask(id: Int): DomainResult<GameTaskDomain> = DomainResult.Success(tasks.first())

    override suspend fun createTask(params: CreateTaskParams): DomainResult<GameTaskDomain> =
        DomainResult.Failure(GameError.Unknown("Not used in this test"))

    override suspend fun completeTask(taskId: Int): DomainResult<TaskCompletionResult> =
        DomainResult.Failure(GameError.Unknown("Not used in this test"))

    override suspend fun failTask(taskId: Int): DomainResult<TaskFailureResult> =
        DomainResult.Failure(GameError.Unknown("Not used in this test"))

    override suspend fun checkOverdueTasks(heroId: Int): DomainResult<OverdueResult> =
        DomainResult.Success(OverdueResult(overdueCount = 0, penalties = emptyList(), message = ""))

    override suspend fun deleteTask(taskId: Int): DomainResult<Unit> = DomainResult.Success(Unit)

    override suspend fun retryTaskSync(taskId: Int): DomainResult<Unit> = DomainResult.Success(Unit)

    override suspend fun deleteLocalTask(taskId: Int): DomainResult<Unit> = DomainResult.Success(Unit)
}
