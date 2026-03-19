package com.lifetracker.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.lifetracker.mobile.navigation.NavGraph
import com.lifetracker.mobile.ui.model.UiEvent
import com.lifetracker.mobile.ui.theme.AppBackground
import com.lifetracker.mobile.ui.theme.LifeTrackerMobileTheme
import com.lifetracker.mobile.ui.viewmodel.HeroViewModel
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LifeTrackerMobileTheme {
                val vm = koinViewModel<HeroViewModel>()
                val state by vm.state.collectAsState()
                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(Unit) {
                    vm.events.collect { event ->
                        when (event) {
                            is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                            is UiEvent.TaskCompleted -> snackbarHostState.showSnackbar(event.message)
                            is UiEvent.TaskFailed -> snackbarHostState.showSnackbar(event.message)
                            is UiEvent.HeroRespawned -> snackbarHostState.showSnackbar(event.message)
                            is UiEvent.HeroHealed -> snackbarHostState.showSnackbar(event.message)
                            is UiEvent.TaskCreated -> navController.popBackStack()
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize()
                    .background(AppBackground)
                    .windowInsetsPadding(WindowInsets.systemBars)
                ) {
                    NavGraph(navController = navController, vm = vm, state = state)
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 80.dp)
                    )
                }
            }
        }
    }
}

