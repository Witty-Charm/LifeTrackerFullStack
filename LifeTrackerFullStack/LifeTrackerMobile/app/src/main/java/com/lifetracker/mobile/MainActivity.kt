package com.lifetracker.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.lifetracker.mobile.navigation.NavGraph
import com.lifetracker.mobile.ui.theme.LifeTrackerMobileTheme
import com.lifetracker.mobile.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LifeTrackerMobileTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: MainViewModel = viewModel()
                    val navController = rememberNavController()
                    
                    val hero by viewModel.hero.collectAsState(initial = null)
                    val tasks by viewModel.tasks.collectAsState(initial = emptyList())
                    
                    NavGraph(
                        navController = navController,
                        hero = hero,
                        tasks = tasks,
                        onAddTask = { title, description, xpReward ->
                            viewModel.addTask(title, description, xpReward)
                        },
                        onCompleteTask = { task ->
                            viewModel.completeTask(task)
                        }
                    )
                }
            }
        }
    }
}

