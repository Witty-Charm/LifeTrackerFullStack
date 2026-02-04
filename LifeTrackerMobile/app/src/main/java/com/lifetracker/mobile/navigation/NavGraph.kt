package com.lifetracker.mobile.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.lifetracker.mobile.ui.screens.AddTaskScreen
import com.lifetracker.mobile.ui.screens.HomeScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object AddTask : Screen("add_task")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    hero: com.lifetracker.mobile.data.Hero?,
    tasks: List<com.lifetracker.mobile.data.GameTask>,
    onAddTask: (String, String?, Int) -> Unit,
    onCompleteTask: (com.lifetracker.mobile.data.GameTask) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                hero = hero,
                tasks = tasks,
                onAddTaskClick = {
                    navController.navigate(Screen.AddTask.route)
                },
                onCompleteTask = onCompleteTask
            )
        }
        
        composable(Screen.AddTask.route) {
            AddTaskScreen(
                onSave = { title, description, xpReward ->
                    onAddTask(title, description, xpReward)
                    navController.popBackStack()
                },
                onCancel = {
                    navController.popBackStack()
                }
            )
        }
    }
}

