package com.lifetracker.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.lifetracker.mobile.navigation.Screen
import com.lifetracker.mobile.ui.components.DailyObjectiveCard
import com.lifetracker.mobile.ui.components.GameBottomNavigationBar
import com.lifetracker.mobile.ui.components.HeroSection
import com.lifetracker.mobile.ui.components.HomeTab
import com.lifetracker.mobile.ui.components.TaskItem
import com.lifetracker.mobile.ui.model.HeroScreenState
import com.lifetracker.mobile.ui.model.UiEvent
import com.lifetracker.mobile.ui.model.UiTaskType
import com.lifetracker.mobile.ui.model.isHealLoading
import com.lifetracker.mobile.ui.model.isRespawnLoading
import com.lifetracker.mobile.ui.model.isTaskLoading
import com.lifetracker.mobile.ui.viewmodel.HeroViewModel
import com.lifetracker.mobile.ui.viewmodel.ShopViewModel
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HeroScreenState,
    vm: HeroViewModel,
    navController: NavController,
    createdTaskType: UiTaskType?,
) {
    val selectedTabState = remember { mutableStateOf(HomeTab.ToDos) }
    val selectedTab by selectedTabState
    val onTabSelected: (HomeTab) -> Unit = remember { { selectedTabState.value = it } }
    val hazeState = rememberHazeState()

    val shopVm: ShopViewModel = koinViewModel()
    val shopState by shopVm.state.collectAsStateWithLifecycle()
    val shopSnackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.hero?.id) {
        state.hero?.id?.let { shopVm.loadForHero(it) }
    }

    LaunchedEffect(state.hero?.gold) {
        state.hero?.gold?.let { shopVm.refreshWithGold(it) }
    }

    LaunchedEffect(Unit) {
        shopVm.events.collect { event ->
            when (event) {
                is UiEvent.HeroGoldUpdated -> vm.updateHeroGold(event.newGold)
                is UiEvent.HeroHpUpdated -> vm.updateHeroHp(event.newHp, event.maxHp)
                is UiEvent.HeroXpBoostUpdated -> vm.updateHeroXpBoost(event.percent, event.tasksRemaining)
                is UiEvent.ShowSnackbar -> launch { shopSnackbar.showSnackbar(event.message) }
                else -> Unit
            }
        }
    }

    LaunchedEffect(createdTaskType) {
        createdTaskType?.let {
            selectedTabState.value = when (it) {
                UiTaskType.Habit -> HomeTab.Habits
                UiTaskType.OneTime -> HomeTab.ToDos
                UiTaskType.Daily -> HomeTab.Dailies
                UiTaskType.Unknown -> HomeTab.ToDos
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(
                hostState = shopSnackbar,
                modifier = Modifier.padding(bottom = 80.dp)
            )
        },
        topBar = {
            var showMenu by remember { mutableStateOf(false) }
            TopAppBar(
                title = { },
                modifier = Modifier.hazeEffect(state = hazeState),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Rewards") },
                            onClick = { showMenu = false },
                            leadingIcon = { Icon(Icons.Filled.EmojiEvents, contentDescription = null) },
                        )
                    }
                }
            )
        },
        bottomBar = {
            val heroId = state.hero?.id
            val route = if (selectedTab == HomeTab.Dailies && heroId != null) {
                Screen.CreateDaily.route(heroId)
            } else {
                Screen.CreateTask.route
            }
            GameBottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                hazeState = hazeState,
                onAddClick = { if (selectedTab != HomeTab.Shop) navController.navigate(route) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                    end = innerPadding.calculateEndPadding(LocalLayoutDirection.current),
                    bottom = innerPadding.calculateBottomPadding(),
                    top = (innerPadding.calculateTopPadding() - 28.dp).coerceAtLeast(0.dp),
                )
        ) {
            when {
                state.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                state.criticalError != null -> {
                    ErrorView(onRetry = { vm.loadData() })
                }
                else -> {
                    val onHeal = remember { { vm.healHero() } }
                    val onRespawn = remember { { vm.respawnHero() } }

                    if (selectedTab == HomeTab.Shop) {
                        val hero = state.hero ?: return@Column
                        ShopScreen(
                            state = shopState,
                            hero = hero,
                            onBuy = { itemId -> shopVm.buyItem(hero.id, itemId, hero.gold) },
                            onShowInventory = shopVm::showInventory,
                            snackbarHostState = shopSnackbar,
                        )
                    } else {
                        state.hero?.let { hero ->
                            HeroSection(
                                hero = hero,
                                onHeal = onHeal,
                                onRespawn = onRespawn,
                                isHealLoading = state.isHealLoading,
                                isRespawnLoading = state.isRespawnLoading,
                            )
                            DailyObjectiveCard(hero = hero, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
                        }
                        val filteredTasks = remember(selectedTab, state.tasks) {
                            when (selectedTab) {
                                HomeTab.Habits  -> state.tasks.filter { it.type == UiTaskType.Habit }
                                HomeTab.ToDos   -> state.tasks.filter { it.type == UiTaskType.OneTime }
                                HomeTab.Dailies -> state.tasks.filter { it.type == UiTaskType.Daily }
                                else -> emptyList()
                            }
                        }
                        if (filteredTasks.isEmpty() && !state.isLoading) {
                            EmptyTasksPlaceholder()
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(filteredTasks, key = { it.id }) { task ->
                                    val taskId = task.id
                                    val onComplete = remember(taskId) { { vm.completeTask(taskId) } }
                                    val onFail = remember(taskId) { { vm.failTask(taskId) } }
                                    val onDelete = remember(taskId) { { vm.deleteTask(taskId) } }
                                    val onRetry = remember(taskId) { { vm.retrySync(taskId) } }
                                    val onDeleteFailed = remember(taskId) { { vm.deleteFailedTask(taskId) } }
                                    TaskItem(
                                        task = task,
                                        onCompleteClick = onComplete,
                                        onFailClick = onFail,
                                        onDeleteClick = onDelete,
                                        isActionLoading = state.isTaskLoading(taskId),
                                        onRetrySyncClick = onRetry,
                                        onDeleteFailedTaskClick = onDeleteFailed,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorView(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Something went wrong", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
            Text("Retry", color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
private fun EmptyTasksPlaceholder() {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text("No tasks here yet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
