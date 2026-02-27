package com.lifetracker.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.lifetracker.mobile.ui.model.UiTaskType
import com.lifetracker.mobile.navigation.Screen
import com.lifetracker.mobile.ui.components.DailyObjectiveCard
import com.lifetracker.mobile.ui.components.GameBottomNavigationBar
import com.lifetracker.mobile.ui.components.HeroSection
import com.lifetracker.mobile.ui.components.HomeTab
import com.lifetracker.mobile.ui.components.TaskItem
import com.lifetracker.mobile.ui.model.HeroScreenState
import com.lifetracker.mobile.ui.theme.AppBackground
import com.lifetracker.mobile.ui.theme.PurpleAccent
import com.lifetracker.mobile.ui.theme.TextPrimary
import com.lifetracker.mobile.ui.theme.TextSecondary
import com.lifetracker.mobile.ui.viewmodel.HeroViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HeroScreenState,
    vm: HeroViewModel,
    navController: NavController
) {
    var selectedTab by remember { mutableStateOf(HomeTab.ToDos) }

    Scaffold(
        containerColor = AppBackground,
        bottomBar = {
            GameBottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.CreateTask.route) },
                shape = androidx.compose.foundation.shape.CircleShape,
                containerColor = PurpleAccent,
                contentColor = TextPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add task",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PurpleAccent)
                    }
                }

                state.criticalError != null -> {
                    ErrorView(onRetry = { vm.loadData() })
                }

                else -> {
                    state.hero?.let { hero ->
                        HeroSection(
                            hero = hero,
                            onHeal = { vm.healHero() },
                            onRespawn = { vm.respawnHero() },
                        )
                        DailyObjectiveCard(
                            hero = hero,
                            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                        )
                    }

                    val filteredTasks = when (selectedTab) {
                        HomeTab.Habits -> state.tasks.filter { it.type == UiTaskType.Habit }
                        HomeTab.ToDos -> state.tasks.filter { it.type == UiTaskType.OneTime }
                        else -> emptyList()
                    }

                    if (filteredTasks.isEmpty() && !state.isLoading) {
                        EmptyTasksPlaceholder()
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize(),
                            contentPadding = PaddingValues(
                                horizontal = 16.dp,
                                vertical = 4.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredTasks, key = { it.id }) { task ->
                                TaskItem(
                                    task = task,
                                    onCompleteClick = { vm.completeTask(task.id) },
                                    onFailClick = { vm.failTask(task.id) },
                                    isActionLoading = state.isActionLoading,
                                    onDeleteClick = { vm.deleteTask(task.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorView(
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Something went wrong",
            style = MaterialTheme.typography.titleMedium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
        ) {
            Text(text = "Retry", color = TextPrimary)
        }
    }
}

@Composable
private fun EmptyTasksPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No tasks here yet",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}
