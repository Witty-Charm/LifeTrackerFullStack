package com.lifetracker.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.lifetracker.mobile.core.theme.ThemeController
import com.lifetracker.mobile.navigation.NavGraph
import com.lifetracker.mobile.ui.components.GlassmorphismSnackbar
import com.lifetracker.mobile.ui.model.UiEvent
import com.lifetracker.mobile.ui.snackbar.TaskActionSnackbarBatcher
import com.lifetracker.mobile.ui.theme.LifeTrackerMobileTheme
import com.lifetracker.mobile.ui.viewmodel.HeroViewModel
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val vm: HeroViewModel by viewModel()
    private var hasStarted = false

    override fun onStart() {
        super.onStart()
        if (hasStarted) {
            vm.refreshOnForeground()
        } else {
            hasStarted = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeController: ThemeController = get()
            val themeMode by themeController.themeMode.collectAsState()
            LifeTrackerMobileTheme(themeMode = themeMode) {
                val state by vm.state.collectAsState()
                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }
                val taskActionBatcher = remember { TaskActionSnackbarBatcher() }
                val hazeState = rememberHazeState()

                LaunchedEffect(Unit) {
                    var flushJob: Job? = null

                    suspend fun flushTaskBatch() {
                        taskActionBatcher.flush()?.let { snackbarHostState.showSnackbar(it) }
                    }

                    vm.events.collect { event ->
                        when (event) {
                            is UiEvent.ShowSnackbar -> {
                                flushJob?.cancel()
                                flushTaskBatch()
                                snackbarHostState.showSnackbar(event.message)
                            }

                            is UiEvent.TaskAction -> {
                                taskActionBatcher.enqueue(event.feedback)
                                flushJob?.cancel()
                                flushJob =
                                    launch {
                                        delay(1_500)
                                        flushTaskBatch()
                                    }
                            }

                            is UiEvent.UndoDeletePrompt -> {
                                flushJob?.cancel()
                                flushTaskBatch()
                                val result =
                                    snackbarHostState.showSnackbar(
                                        message = event.message,
                                        actionLabel = "UNDO",
                                        withDismissAction = false,
                                        duration = SnackbarDuration.Short,
                                    )
                                when (result) {
                                    SnackbarResult.ActionPerformed -> vm.undoDeleteTask(event.taskId)
                                    SnackbarResult.Dismissed -> vm.confirmDeleteTask(event.taskId)
                                }
                            }

                            is UiEvent.HeroRespawned -> {
                                flushJob?.cancel()
                                flushTaskBatch()
                                snackbarHostState.showSnackbar(event.message)
                            }

                            is UiEvent.HeroHealed -> {
                                flushJob?.cancel()
                                flushTaskBatch()
                                snackbarHostState.showSnackbar(event.message)
                            }

                            is UiEvent.TaskCreated -> {
                                navController.previousBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("task_changed", true)
                                navController.popBackStack()
                            }

                            is UiEvent.TaskUpdated -> {
                                navController.previousBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("task_changed", true)
                                navController.popBackStack()
                            }

                            is UiEvent.HeroGoldUpdated -> {
                                Unit
                            }

                            is UiEvent.HeroHpUpdated -> {
                                Unit
                            }

                            is UiEvent.HeroXpBoostUpdated -> {
                                Unit
                            }

                            is UiEvent.HeroRecoveryUpdated -> {
                                Unit
                            }

                            UiEvent.RefreshTasks -> {
                                Unit
                            }
                        }
                    }
                }

                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .hazeSource(hazeState),
                ) {
                    NavGraph(navController = navController, vm = vm, state = state)
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier =
                            Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 140.dp),
                        snackbar = { GlassmorphismSnackbar(it, hazeState) },
                    )
                }
            }
        }
    }
}
