package com.lifetracker.mobile.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.lifetracker.mobile.ui.screens.CreateHeroScreen
import com.lifetracker.mobile.ui.screens.CreateTaskScreen
import com.lifetracker.mobile.ui.screens.HomeScreen
import com.lifetracker.mobile.ui.model.HeroScreenState
import com.lifetracker.mobile.ui.viewmodel.HeroViewModel

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object CreateHero : Screen("create_hero")
    object CreateTask : Screen("create_task")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    vm: HeroViewModel,
    state: HeroScreenState
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
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                state = state,
                vm = vm,
                navController = navController
            )
        }

        composable(Screen.CreateTask.route) {
            CreateTaskScreen(state = state, vm = vm, navController = navController)
        }

        composable(Screen.CreateHero.route) {
            CreateHeroScreen(state = state, vm = vm, navController = navController)
        }
    }
}
