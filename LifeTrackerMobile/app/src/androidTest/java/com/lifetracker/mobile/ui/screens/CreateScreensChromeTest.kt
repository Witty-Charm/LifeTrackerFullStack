package com.lifetracker.mobile.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkManager
import com.lifetracker.mobile.core.reminder.ReminderScheduler
import com.lifetracker.mobile.core.serialization.JsonDefaults
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
import com.lifetracker.mobile.domain.model.TaskFailureResult
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
import com.lifetracker.mobile.ui.model.CreateDailyFormState
import com.lifetracker.mobile.ui.model.HeroScreenState
import com.lifetracker.mobile.ui.model.RepeatFrequency
import com.lifetracker.mobile.ui.model.UiTaskType
import com.lifetracker.mobile.ui.theme.LifeTrackerMobileTheme
import com.lifetracker.mobile.ui.viewmodel.CreateDailyViewModel
import com.lifetracker.mobile.ui.viewmodel.HeroViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CreateScreensChromeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun createTaskScreen_showsBackAndBottomSave_insteadOfTopBarSave() {
        composeRule.setContent {
            LifeTrackerMobileTheme {
                CreateTaskScreen(
                    state = HeroScreenState(),
                    vm = rememberHeroViewModel(),
                    navController = rememberNavController(),
                )
            }
        }

        composeRule.onNodeWithTag("create_top_bar").assertIsDisplayed()
        composeRule.onNodeWithText("Create task").assertIsDisplayed()
        composeRule.onNodeWithText("Task name").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back").assertIsDisplayed()
        composeRule.onNodeWithTag("create_primary_action_footer").assertIsDisplayed()
        composeRule.onAllNodesWithText("Save").assertCountEquals(1)
    }

    @Test
    fun createTaskScreen_showsHabitChrome_andHidesTypeSelection_whenLockedToHabit() {
        composeRule.setContent {
            LifeTrackerMobileTheme {
                CreateTaskScreen(
                    state = HeroScreenState(),
                    vm = rememberHeroViewModel(),
                    navController = rememberNavController(),
                    initialType = UiTaskType.Habit,
                    lockTypeSelection = true,
                )
            }
        }

        composeRule.onNodeWithTag("create_top_bar").assertIsDisplayed()
        composeRule.onNodeWithText("Create habit").assertIsDisplayed()
        composeRule.onNodeWithText("Habit name").assertIsDisplayed()
        composeRule.onAllNodesWithText("Type").assertCountEquals(0)
        composeRule.onAllNodesWithText("One Time").assertCountEquals(0)
        composeRule.onNodeWithTag("create_primary_action_footer").assertIsDisplayed()
        composeRule.onAllNodesWithText("Save").assertCountEquals(1)
    }

    @Test
    fun createTaskScreen_showsTodoChrome_andHidesTypeSelection_whenLockedToOneTime() {
        composeRule.setContent {
            LifeTrackerMobileTheme {
                CreateTaskScreen(
                    state = HeroScreenState(),
                    vm = rememberHeroViewModel(),
                    navController = rememberNavController(),
                    initialType = UiTaskType.OneTime,
                    lockTypeSelection = true,
                )
            }
        }

        composeRule.onNodeWithTag("create_top_bar").assertIsDisplayed()
        composeRule.onNodeWithText("Create to do").assertIsDisplayed()
        composeRule.onNodeWithText("To do name").assertIsDisplayed()
        composeRule.onAllNodesWithText("Type").assertCountEquals(0)
        composeRule.onAllNodesWithText("Habit").assertCountEquals(0)
        composeRule.onNodeWithTag("create_primary_action_footer").assertIsDisplayed()
        composeRule.onAllNodesWithText("Save").assertCountEquals(1)
    }

    @Test
    fun createTaskScreen_showsHabitPolarityChips_forHabitAndHidesForTodo() {
        composeRule.setContent {
            LifeTrackerMobileTheme {
                CreateTaskScreen(
                    state = HeroScreenState(),
                    vm = rememberHeroViewModel(),
                    navController = rememberNavController(),
                    initialType = UiTaskType.Habit,
                    lockTypeSelection = true,
                )
            }
        }

        composeRule.onNodeWithText("Polarity").assertIsDisplayed()
        composeRule.onNodeWithText("Positive").assertIsDisplayed()
        composeRule.onNodeWithText("Negative").assertIsDisplayed()
        composeRule.onNodeWithText("Both").assertIsDisplayed()

        composeRule.setContent {
            LifeTrackerMobileTheme {
                CreateTaskScreen(
                    state = HeroScreenState(),
                    vm = rememberHeroViewModel(),
                    navController = rememberNavController(),
                    initialType = UiTaskType.OneTime,
                    lockTypeSelection = true,
                )
            }
        }

        composeRule.onAllNodesWithText("Polarity").assertCountEquals(0)
        composeRule.onAllNodesWithText("Positive").assertCountEquals(0)
        composeRule.onAllNodesWithText("Negative").assertCountEquals(0)
        composeRule.onAllNodesWithText("Both").assertCountEquals(0)
    }

    @Test
    fun createDailyScreen_showsBackBottomSave_andImportedStreakCopy() {
        composeRule.setContent {
            LifeTrackerMobileTheme {
                CreateDailyScreen(
                    state = CreateDailyFormState(title = "Daily", frequency = RepeatFrequency.DAILY, interval = 1),
                    vm = rememberCreateDailyViewModel(),
                    navController = rememberNavController(),
                )
            }
        }

        composeRule.onNodeWithTag("create_top_bar").assertIsDisplayed()
        composeRule.onNodeWithText("Create daily").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back").assertIsDisplayed()
        composeRule.onNodeWithTag("create_primary_action_footer").assertIsDisplayed()
        composeRule.onNodeWithText("Imported streak").assertIsDisplayed()
        composeRule.onAllNodesWithText("Save").assertCountEquals(1)
    }

    @Test
    fun createDailyScreen_onlyOffersDailyCadence() {
        composeRule.setContent {
            LifeTrackerMobileTheme {
                CreateDailyScreen(
                    state = CreateDailyFormState(title = "Daily", frequency = RepeatFrequency.DAILY, interval = 1),
                    vm = rememberCreateDailyViewModel(),
                    navController = rememberNavController(),
                )
            }
        }

        composeRule.onNodeWithText("Repeats DAILY every 1 Days").assertIsDisplayed()
        composeRule.onAllNodesWithText("Weekly").assertCountEquals(0)
        composeRule.onAllNodesWithText("Monthly").assertCountEquals(0)
        composeRule.onAllNodesWithText("Yearly").assertCountEquals(0)
    }

    @Test
    fun createHeroScreen_showsBackAndBottomCreate() {
        composeRule.setContent {
            LifeTrackerMobileTheme {
                CreateHeroScreen(
                    state = HeroScreenState(),
                    vm = rememberHeroViewModel(),
                    navController = rememberNavController(),
                )
            }
        }

        composeRule.onNodeWithTag("create_top_bar").assertIsDisplayed()
        composeRule.onNodeWithText("Create hero").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back").assertIsDisplayed()
        composeRule.onNodeWithTag("create_primary_action_footer").assertIsDisplayed()
        composeRule.onAllNodesWithText("Create").assertCountEquals(1)
    }
}

@Composable
private fun rememberHeroViewModel(): HeroViewModel {
    val context = LocalContext.current

    return androidx.compose.runtime.remember {
        val heroRepository = CreateScreenFakeHeroRepository()
        val taskRepository = CreateScreenFakeTaskRepository()

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
                    createTask = CreateTaskUseCase(taskRepository),
                    completeTask = CompleteTaskUseCase(taskRepository),
                    failTask = FailTaskUseCase(taskRepository),
                    checkOverdue = CheckOverdueTasksUseCase(taskRepository),
                    retryTaskSync = RetryTaskSyncUseCase(taskRepository),
                    deleteLocalTask = DeleteLocalTaskUseCase(taskRepository),
                    deleteTask = DeleteTaskUseCase(taskRepository),
                ),
            workManager = WorkManager.getInstance(context),
        )
    }
}

@Composable
private fun rememberCreateDailyViewModel(): CreateDailyViewModel {
    val context = LocalContext.current

    return androidx.compose.runtime.remember {
        val taskRepository = CreateScreenFakeTaskRepository()
        CreateDailyViewModel(
            heroId = 1,
            taskUseCases =
                TaskUseCases(
                    getTask = GetTaskUseCase(taskRepository),
                    getTasks = GetTasksUseCase(taskRepository),
                    createTask = CreateTaskUseCase(taskRepository),
                    completeTask = CompleteTaskUseCase(taskRepository),
                    failTask = FailTaskUseCase(taskRepository),
                    checkOverdue = CheckOverdueTasksUseCase(taskRepository),
                    retryTaskSync = RetryTaskSyncUseCase(taskRepository),
                    deleteLocalTask = DeleteLocalTaskUseCase(taskRepository),
                    deleteTask = DeleteTaskUseCase(taskRepository),
                ),
            reminderScheduler = ReminderScheduler(WorkManager.getInstance(context), JsonDefaults),
        )
    }
}

private class CreateScreenFakeHeroRepository : HeroRepository {
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

private class CreateScreenFakeTaskRepository : TaskRepository {
    override suspend fun getTasks(heroId: Int): DomainResult<List<GameTaskDomain>> = DomainResult.Success(emptyList())

    override suspend fun getTask(id: Int): DomainResult<GameTaskDomain> = DomainResult.Failure(GameError.Unknown("task"))

    override suspend fun createTask(params: CreateTaskParams): DomainResult<GameTaskDomain> =
        DomainResult.Success(
            GameTaskDomain(
                id = 1,
                heroId = params.heroId,
                title = params.title,
                description = params.description.orEmpty(),
                type = params.type,
                difficulty = params.difficulty,
                habitPolarity = params.habitPolarity,
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
                baseXp = 10,
                baseGold = 5,
                hpPenalty = 0,
                goldPenalty = 0,
                streak = null,
                pendingSync = false,
                syncError = null,
            ),
        )

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
