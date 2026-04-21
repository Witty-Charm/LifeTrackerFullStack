package com.lifetracker.mobile.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifetracker.mobile.ui.model.AchievementUi
import com.lifetracker.mobile.ui.model.AchievementsScreenState
import com.lifetracker.mobile.ui.viewmodel.AchievementsViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreenRoot(
    heroId: Int,
    onBack: () -> Unit = {},
    viewModel: AchievementsViewModel = koinViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value

    LaunchedEffect(heroId) {
        viewModel.loadAchievements(heroId)
    }

    AchievementsScreen(state = state, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    state: AchievementsScreenState,
    onBack: () -> Unit = {},
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            RewardsTopBar(onBack = onBack)
        },
    ) { padding ->
        when {
            state.isLoading -> {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            else -> {
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.achievements, key = { it.key }) { achievement ->
                        AchievementCard(achievement)
                    }
                }
            }
        }
    }
}

@Composable
private fun RewardsTopBar(onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 8.dp,
                ),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = colors.surface,
            tonalElevation = 6.dp,
            shadowElevation = 10.dp,
            border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.18f)),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            brush =
                                Brush.verticalGradient(
                                    colors =
                                        listOf(
                                            colors.primary.copy(alpha = 0.18f),
                                            colors.surface,
                                        ),
                                ),
                        ).padding(horizontal = 12.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier.align(Alignment.CenterStart),
                    shape = CircleShape,
                    color = colors.primary.copy(alpha = 0.14f),
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.onSurface,
                        )
                    }
                }

                Text(
                    text = "Rewards",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface,
                )
            }
        }
    }
}

@Composable
private fun AchievementCard(achievement: AchievementUi) {
    val colors = MaterialTheme.colorScheme
    val accentAlpha = if (achievement.unlocked) 0.22f else 0.12f

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.16f)),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        brush =
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        colors.primary.copy(alpha = accentAlpha),
                                        colors.surface,
                                    ),
                            ),
                    ).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                achievement.title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
            )
            Text(
                achievement.description,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            Text(
                "+${achievement.goldReward} Gold",
                style = MaterialTheme.typography.bodySmall,
                color = colors.secondary,
            )
            Text(
                if (achievement.unlocked) "Unlocked" else "Locked",
                style = MaterialTheme.typography.labelMedium,
                color = if (achievement.unlocked) colors.primary else colors.onSurfaceVariant,
            )
        }
    }
}
