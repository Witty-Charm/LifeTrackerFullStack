package com.lifetracker.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.lifetracker.mobile.navigation.Screen
import com.lifetracker.mobile.ui.components.CreateScreenFloatingFooter
import com.lifetracker.mobile.ui.components.CreateScreenTopBar
import com.lifetracker.mobile.ui.mapper.toMessage
import com.lifetracker.mobile.ui.model.HeroScreenState
import com.lifetracker.mobile.ui.model.isAnyActionLoading
import com.lifetracker.mobile.ui.viewmodel.HeroViewModel

@Composable
fun CreateHeroScreen(
    state: HeroScreenState,
    vm: HeroViewModel,
    navController: NavController,
) {
    var name by remember { mutableStateOf("") }

    LaunchedEffect(state.needsHeroCreation) {
        if (!state.needsHeroCreation && state.hero != null) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.CreateHero.route) { inclusive = true }
            }
        }
    }

    Scaffold(
        topBar = {
            CreateScreenTopBar(
                title = "Create hero",
                onBack = { navController.popBackStack() },
            )
        },
        bottomBar = {
            CreateScreenFloatingFooter(
                actionLabel = "Create",
                enabled = name.isNotBlank() && !state.isAnyActionLoading,
                onClick = { vm.createHero(name) },
                isLoading = state.isAnyActionLoading,
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Hero name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            val context = LocalContext.current
            val errorText = state.actionError?.toMessage(context)
            if (errorText != null) {
                Text(
                    text = errorText,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
