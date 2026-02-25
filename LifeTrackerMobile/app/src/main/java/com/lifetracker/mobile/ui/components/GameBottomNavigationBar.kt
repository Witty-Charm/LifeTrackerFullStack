package com.lifetracker.mobile.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lifetracker.mobile.ui.theme.PurpleAccent
import com.lifetracker.mobile.ui.theme.SurfaceDark
import com.lifetracker.mobile.ui.theme.TextSecondary

enum class HomeTab(val label: String) {
    Habits("Habits"),
    Dailies("Dailies"),
    ToDos("To Do's"),
    Rewards("Rewards")
}

@Composable
fun GameBottomNavigationBar(
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit
) {
    NavigationBar(
        containerColor = SurfaceDark,
        tonalElevation = 0.dp,
        windowInsets = WindowInsets(0)
    ) {
        HomeTab.entries.forEach { tab ->
            val icon = when (tab) {
                HomeTab.Habits -> Icons.Default.FitnessCenter
                HomeTab.Dailies -> Icons.Default.CalendarToday
                HomeTab.ToDos -> Icons.Default.CheckCircleOutline
                HomeTab.Rewards -> Icons.Default.EmojiEvents
            }

            NavigationBarItem(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                icon = { Icon(imageVector = icon, contentDescription = tab.label) },
                label = { Text(text = tab.label, style = MaterialTheme.typography.bodySmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PurpleAccent,
                    selectedTextColor = PurpleAccent,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

