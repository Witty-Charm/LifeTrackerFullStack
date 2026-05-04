package com.lifetracker.mobile.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.lifetracker.mobile.ui.model.HeroScreenState
import com.lifetracker.mobile.ui.model.UiTaskType
import com.lifetracker.mobile.ui.screens.AchievementsScreenRoot
import com.lifetracker.mobile.ui.screens.CreateDailyScreen
import com.lifetracker.mobile.ui.screens.CreateHeroScreen
import com.lifetracker.mobile.ui.screens.CreateTaskScreen
import com.lifetracker.mobile.ui.screens.HomeScreen
import com.lifetracker.mobile.ui.screens.SettingsScreen
import com.lifetracker.mobile.ui.screens.StatsScreenRoot
import com.lifetracker.mobile.ui.viewmodel.CreateDailyViewModel
import com.lifetracker.mobile.ui.viewmodel.HeroViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

sealed interface Screen {
    val route: String

    data object Home : Screen {
        override val route = "home"
    }

    data object CreateHero : Screen {
        override val route = "create_hero"
    }

    data object CreateTask : Screen {
        private const val baseRoute = "create_task"
        const val initialTypeArg = "initialType"
        const val taskIdArg = "taskId"

        override val route = "$baseRoute?$initialTypeArg={$initialTypeArg}&$taskIdArg={$taskIdArg}"

        fun route(initialType: UiTaskType) =
            "$baseRoute?$initialTypeArg=${initialType.name}"

        fun routeForEdit(taskId: Int) =
            "$baseRoute?$taskIdArg=$taskId"

        fun defaultRoute() = baseRoute
    }

    data object Achievements : Screen {
        override val route = "achievements/{heroId}"

        fun route(heroId: Int) = "achievements/$heroId"
    }

    data object Stats : Screen {
        override val route = "stats/{heroId}"

        fun route(heroId: Int) = "stats/$heroId"
    }

    data object Settings : Screen {
        override val route = "settings"
    }

    data object CreateDaily : Screen {
        const val taskIdArg = "taskId"

        override val route = "create_daily/{heroId}?$taskIdArg={$taskIdArg}"

        fun route(heroId: Int) = "create_daily/$heroId"

        fun routeForEdit(heroId: Int, taskId: Int) =
            "create_daily/$heroId?$taskIdArg=$taskId"
    }
}

@Composable
fun NavGraph(
    navController: NavHostController,
    vm: HeroViewModel,
    state: HeroScreenState,
) {
    LaunchedEffect(state.needsHeroCreation) {
        if (state.needsHeroCreation) {
            navController.navigate(Screen.CreateHero.route) {
                popUpTo(Screen.Home.route) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
    ) {
        composable(Screen.Home.route) {
            val taskChanged =
                navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.remove<Boolean>("task_changed")

            LaunchedEffect(taskChanged) {
                if (taskChanged == true) {
                    vm.refreshTasks()
                }
            }

            HomeScreen(
                state = state,
                vm = vm,
                navController = navController,
            )
        }

        composable(
            route = Screen.CreateTask.route,
            arguments =
                listOf(
                    navArgument(Screen.CreateTask.initialTypeArg) {
                        type = NavType.StringType
                        nullable = true
                    },
                    navArgument(Screen.CreateTask.taskIdArg) {
                        type = NavType.StringType
                        nullable = true
                    },
                ),
        ) { backStackEntry ->
            val rawInitialType = backStackEntry.arguments?.getString(Screen.CreateTask.initialTypeArg)
            val rawTaskId = backStackEntry.arguments?.getString(Screen.CreateTask.taskIdArg)
            val editingTaskId = rawTaskId?.toIntOrNull()
            val initialType =
                when (rawInitialType) {
                    UiTaskType.Habit.name -> UiTaskType.Habit
                    UiTaskType.OneTime.name -> UiTaskType.OneTime
                    else -> UiTaskType.OneTime
                }

            CreateTaskScreen(
                state = state,
                vm = vm,
                navController = navController,
                initialType = initialType,
                lockTypeSelection = rawInitialType != null || editingTaskId != null,
                editingTaskId = editingTaskId,
            )
        }

        composable(Screen.CreateHero.route) {
            CreateHeroScreen(state = state, vm = vm, navController = navController)
        }

        composable(
            route = Screen.Achievements.route,
            arguments = listOf(navArgument("heroId") { type = NavType.IntType }),
        ) { backStackEntry ->
            val heroId = backStackEntry.arguments?.getInt("heroId") ?: return@composable
            AchievementsScreenRoot(heroId = heroId, onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.Stats.route,
            arguments = listOf(navArgument("heroId") { type = NavType.IntType }),
        ) { backStackEntry ->
            val heroId = backStackEntry.arguments?.getInt("heroId") ?: return@composable
            StatsScreenRoot(heroId = heroId, onBack = { navController.popBackStack() })
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.CreateDaily.route,
            arguments =
                listOf(
                    navArgument("heroId") { type = NavType.IntType },
                    navArgument(Screen.CreateDaily.taskIdArg) {
                        type = NavType.StringType
                        nullable = true
                    },
                ),
        ) { backStackEntry ->
            val heroId = backStackEntry.arguments?.getInt("heroId") ?: return@composable
            val editingTaskId =
                backStackEntry.arguments
                    ?.getString(Screen.CreateDaily.taskIdArg)
                    ?.toIntOrNull()

            val createDailyVm: CreateDailyViewModel =
                koinViewModel(
                    key = "create_daily_${heroId}_${editingTaskId ?: "new"}",
                    parameters = { parametersOf(heroId, editingTaskId ?: -1) },
                )
            val createDailyState by createDailyVm.state.collectAsStateWithLifecycle()

            CreateDailyScreen(
                state = createDailyState,
                vm = createDailyVm,
                navController = navController,
            )
        }
    }
}
