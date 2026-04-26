package com.lifetracker.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect

@Composable
fun GlassmorphismSnackbar(
    snackbarData: SnackbarData,
    hazeState: HazeState,
) {
    val shape = RoundedCornerShape(20.dp)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDark = isSystemInDarkTheme()
    val borderColor = if (isDark) Color.White else Color.Black

    val glassBg = if (isDark)
        surfaceColor.copy(alpha = 0.70f)
    else
        surfaceColor.copy(alpha = 0.80f)

    val actionLabel = snackbarData.visuals.actionLabel

    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(glassBg)
            .hazeEffect(state = hazeState) {
                backgroundColor = surfaceColor
                blurRadius = 28.dp
                noiseFactor = 0f
            }
            .border(
                width = if (isDark) 0.5.dp else 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        borderColor.copy(alpha = 0.20f),
                        borderColor.copy(alpha = 0.06f),
                    ),
                ),
                shape = shape,
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = snackbarData.visuals.message,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(end = if (actionLabel != null) 8.dp else 0.dp),
            )
            if (actionLabel != null) {
                Text(
                    text = actionLabel,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { snackbarData.performAction() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}