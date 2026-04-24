package com.lifetracker.mobile.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifetracker.mobile.ui.model.StatsScreenState
import com.lifetracker.mobile.ui.viewmodel.StatsViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreenRoot(
    heroId: Int,
    onBack: () -> Unit,
    viewModel: StatsViewModel = koinViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value

    LaunchedEffect(heroId) {
        viewModel.loadStats(heroId)
    }

    StatsScreen(state = state, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    state: StatsScreenState,
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SectionTopBar(title = "Stats", onBack = onBack)
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
                val summaryCards =
                    listOf(
                        StatsMetricCardData("Completed", state.completedCount.toString()),
                        StatsMetricCardData("Failed", state.failedCount.toString()),
                        StatsMetricCardData("Overdue", state.overdueCount.toString()),
                    )
                val taskTypeCards =
                    listOf(
                        StatsMetricCardData("Habits", state.habitCount.toString()),
                        StatsMetricCardData("To dos", state.oneTimeCount.toString()),
                        StatsMetricCardData("Dailies", state.dailyCount.toString()),
                    )
                val heroCards =
                    listOf(
                        StatsMetricCardData("XP", state.xpText),
                        StatsMetricCardData("HP", state.hpText),
                        StatsMetricCardData("Gold", state.goldText),
                        StatsMetricCardData("Daily progress", state.dailyProgressText),
                    )
                val lifetimeCards =
                    listOf(
                        StatsMetricCardData("Total XP earned", state.totalXpEarnedText),
                        StatsMetricCardData("Total gold earned", state.totalGoldEarnedText),
                        StatsMetricCardData("Total gold spent", state.totalGoldSpentText),
                        StatsMetricCardData("Deaths", state.deathCount.toString()),
                        StatsMetricCardData("Active streaks", state.activeStreaks.toString()),
                        StatsMetricCardData("Longest streak", state.longestStreak.toString()),
                    )

                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        StatsSectionCard(
                            title = state.heroName.ifBlank { "Hero" },
                            subtitle = "Level ${state.level}",
                            metrics = heroCards,
                        )
                    }
                    item {
                        StatsSectionCard(
                            title = "Task totals",
                            subtitle = "These totals reflect your current active task list.",
                            metrics = summaryCards,
                        )
                    }
                    item {
                        StatsSectionCard(
                            title = "By task type",
                            subtitle = "Counts are based on active tasks currently on the device.",
                            metrics = taskTypeCards,
                        )
                    }
                    item {
                        StatsSectionCard(
                            title = "Lifetime hero stats",
                            subtitle = "Values come from the current mobile stats snapshot for this hero.",
                            metrics = lifetimeCards,
                        )
                    }
                }
            }
        }
    }
}

data class StatsMetricCardData(
    val label: String,
    val value: String,
)

@Composable
private fun StatsSectionCard(
    title: String,
    subtitle: String,
    metrics: List<StatsMetricCardData>,
) {
    val colors = MaterialTheme.colorScheme

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
                                        colors.primary.copy(alpha = 0.16f),
                                        colors.surface,
                                    ),
                            ),
                    ).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }

            metrics.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowItems.forEach { metric ->
                        StatsMetricCard(
                            modifier = Modifier.weight(1f),
                            metric = metric,
                        )
                    }
                    if (rowItems.size == 1) {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsMetricCard(
    modifier: Modifier = Modifier,
    metric: StatsMetricCardData,
) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.12f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = metric.label,
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSurfaceVariant,
            )
            Text(
                text = metric.value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface,
            )
        }
    }
}
