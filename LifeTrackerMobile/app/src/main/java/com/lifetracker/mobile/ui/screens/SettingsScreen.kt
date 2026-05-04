package com.lifetracker.mobile.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifetracker.mobile.ui.viewmodel.SettingsViewModel
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    vm: SettingsViewModel = koinViewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val hazeState = rememberHazeState()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text("Settings") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                )
            },
        ) { innerPadding ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .hazeSource(hazeState)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                state.accountEmail?.let { email ->
                    Text(
                        text = "Account",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                    Column(
                        modifier = Modifier.padding(vertical = 4.dp),
                    ) {
                        state.accountDisplayName?.takeIf { it.isNotBlank() }?.let { name ->
                            Text(name, style = MaterialTheme.typography.bodyLarge)
                        }
                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !state.isSigningOut) { vm.signOut() }
                            .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = "Sign out",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = state.isSigningOut,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            SignOutOverlay(hazeSource = hazeState)
        }
    }
}

@Composable
private fun SignOutOverlay(hazeSource: dev.chrisbanes.haze.HazeState) {
    val colors = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()
    val cardShape = RoundedCornerShape(24.dp)
    val cardBg =
        if (isDark) colors.surface.copy(alpha = 0.70f) else colors.surface.copy(alpha = 0.85f)
    val borderTint = if (isDark) Color.White else Color.Black

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .pointerInput(Unit) { /* swallow clicks during sign-out */ },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier =
                Modifier
                    .shadow(elevation = 18.dp, shape = cardShape)
                    .clip(cardShape)
                    .background(cardBg)
                    .hazeEffect(state = hazeSource) {
                        backgroundColor = colors.surface
                        blurRadius = 28.dp
                        noiseFactor = 0f
                    }
                    .border(
                        width = if (isDark) 0.5.dp else 1.dp,
                        brush =
                            Brush.verticalGradient(
                                listOf(
                                    borderTint.copy(alpha = 0.22f),
                                    borderTint.copy(alpha = 0.06f),
                                ),
                            ),
                        shape = cardShape,
                    )
                    .padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                strokeWidth = 3.dp,
                color = colors.primary,
            )
            Text(
                text = "Signing out…",
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
            )
        }
    }
}
