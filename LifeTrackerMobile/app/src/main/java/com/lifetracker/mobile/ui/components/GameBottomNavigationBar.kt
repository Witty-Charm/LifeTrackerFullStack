package com.lifetracker.mobile.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifetracker.mobile.ui.theme.PurpleAccent
import com.lifetracker.mobile.ui.theme.SurfaceDark
import com.lifetracker.mobile.ui.theme.TextSecondary
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect

enum class HomeTab(val label: String) {
    Habits("Habits"),
    Dailies("Dailies"),
    ToDos("To Do's"),
    Rewards("Rewards")
}

@Composable
fun GameBottomNavigationBar(
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val tabs = HomeTab.entries
    val selectedIndex = tabs.indexOf(selectedTab)
    val shape = RoundedCornerShape(percent = 50)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .clip(shape)
                .hazeEffect(state = hazeState) {
                    backgroundColor = SurfaceDark
                    blurRadius = 24.dp
                    noiseFactor = 0f
                }
                .border(
                    width = 0.5.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = shape
                )
                .padding(vertical = 6.dp, horizontal = 4.dp)
        ) {
            val tabWidth = maxWidth / tabs.size

            val indicatorOffset by animateDpAsState(
                targetValue = tabWidth * selectedIndex,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
                label = "indicatorOffset"
            )

            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(tabWidth)
                    .height(52.dp)
                    .padding(horizontal = 6.dp)
                    .clip(shape)
                    .background(PurpleAccent.copy(alpha = 0.15f))
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                tabs.forEach { tab ->
                    val isSelected = tab == selectedTab

                    val color by animateColorAsState(
                        targetValue = if (isSelected) PurpleAccent else TextSecondary,
                        label = "tabColor_${tab.name}"
                    )
                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.1f else 1f,
                        label = "tabScale_${tab.name}"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clipToBounds()
                            .padding(top = 4.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onTabSelected(tab) }
                    ) {
                        Icon(
                            imageVector = tabIcon(tab),
                            contentDescription = tab.label,
                            tint = color,
                            modifier = Modifier
                                .size(22.dp)
                                .scale(iconScale)
                        )
                        Text(
                            text = tab.label,
                            fontSize = 11.sp,
                            color = color,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

private fun tabIcon(tab: HomeTab): ImageVector = when (tab) {
    HomeTab.Habits  -> Icons.Default.FitnessCenter
    HomeTab.Dailies -> Icons.Default.CalendarToday
    HomeTab.ToDos   -> Icons.Default.CheckCircleOutline
    HomeTab.Rewards -> Icons.Default.EmojiEvents
}
