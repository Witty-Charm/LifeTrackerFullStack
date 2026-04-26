package com.lifetracker.mobile.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    val shape = RoundedCornerShape(24.dp)
    val surfaceColor = MaterialTheme.colorScheme.surface

    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(shape)
            .hazeEffect(state = hazeState) {
                backgroundColor = surfaceColor
                blurRadius = 28.dp
                noiseFactor = 0f
            }
            .border(
                width = 0.5.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.20f),
                        Color.White.copy(alpha = 0.04f),
                    ),
                ),
                shape = shape,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = snackbarData.visuals.message,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
