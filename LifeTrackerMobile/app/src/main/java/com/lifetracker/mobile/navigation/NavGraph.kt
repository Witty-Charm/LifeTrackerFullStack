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
import com.lifetracker.mobile.ui.screens.CreateDailyScreen
import com.lifetracker.mobile.ui.screens.CreateHeroScreen
import com.lifetracker.mobile.ui.screens.CreateTaskScreen
import com.lifetracker.mobile.ui.screens.HomeScreen
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
        override val route = "create_task"
    }

    data object CreateDaily : Screen {
        override val route = "create_daily/{heroId}"

        fun route(heroId: Int) = "create_daily/$heroId"
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
            val createdTaskType =
                navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.remove<UiTaskType>("task_created")

            LaunchedEffect(createdTaskType) {
                if (createdTaskType != null) {
                    vm.refreshTasks()
                }
            }

            HomeScreen(
                state = state,
                vm = vm,
                navController = navController,
            )
        }

        composable(Screen.CreateTask.route) {
            CreateTaskScreen(state = state, vm = vm, navController = navController)
        }

        composable(Screen.CreateHero.route) {
            CreateHeroScreen(state = state, vm = vm, navController = navController)
        }

        composable(
            route = Screen.CreateDaily.route,
            arguments = listOf(navArgument("heroId") { type = NavType.IntType }),
        ) { backStackEntry ->
            val heroId = backStackEntry.arguments?.getInt("heroId") ?: return@composable

            val createDailyVm: CreateDailyViewModel =
                koinViewModel(
                    parameters = { parametersOf(heroId) },
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
