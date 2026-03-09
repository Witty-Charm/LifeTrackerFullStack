package com.lifetracker.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.lifetracker.mobile.navigation.Screen
import com.lifetracker.mobile.ui.mapper.toMessage
import com.lifetracker.mobile.ui.model.HeroScreenState
import com.lifetracker.mobile.ui.model.isAnyActionLoading
import com.lifetracker.mobile.ui.viewmodel.HeroViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateHeroScreen(
    state: HeroScreenState,
    vm: HeroViewModel,
    navController: NavController
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
            TopAppBar(
                title = { Text("Create hero") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Hero name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            val context = LocalContext.current
            val errorText = state.actionError?.toMessage(context)

            if (errorText != null) {
                Text(
                    text = errorText,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { vm.createHero(name) },
                enabled = name.isNotBlank() && !state.isAnyActionLoading,
                modifier = Modifier.align(Alignment.End)
            ) {
                if (state.isAnyActionLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .height(18.dp)
                            .padding(end = 8.dp),
                        strokeWidth = 2.dp
                    )
                }
                Text("Create")
            }
        }
    }
}

