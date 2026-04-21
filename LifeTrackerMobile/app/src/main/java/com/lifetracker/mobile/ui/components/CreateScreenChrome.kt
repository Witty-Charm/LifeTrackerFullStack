package com.lifetracker.mobile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun CreateScreenTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = statusBarPadding + 4.dp,
                    bottom = 8.dp,
                ).testTag("create_top_bar"),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = colors.surface,
            tonalElevation = 4.dp,
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.14f)),
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
                                            colors.primary.copy(alpha = 0.10f),
                                            colors.surface,
                                        ),
                                ),
                        ).padding(horizontal = 12.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier.align(Alignment.CenterStart),
                    shape = CircleShape,
                    color = colors.primary.copy(alpha = 0.10f),
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
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface,
                )
            }
        }
    }
}

@Composable
fun CreateScreenFloatingFooter(
    actionLabel: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
) {
    val colors = MaterialTheme.colorScheme
    val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = contentPadding.calculateTopPadding(),
                    bottom = navigationBarPadding + contentPadding.calculateBottomPadding(),
                ).testTag("create_primary_action_footer"),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = colors.surface,
            tonalElevation = 6.dp,
            shadowElevation = 10.dp,
            border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.14f)),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            brush =
                                Brush.verticalGradient(
                                    colors =
                                        listOf(
                                            colors.primary.copy(alpha = 0.08f),
                                            colors.surface,
                                        ),
                                ),
                        ).padding(12.dp),
            ) {
                Button(
                    onClick = onClick,
                    enabled = enabled && !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(),
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = colors.onPrimary,
                        )
                    } else {
                        Text(actionLabel)
                    }
                }
            }
        }
    }
}
