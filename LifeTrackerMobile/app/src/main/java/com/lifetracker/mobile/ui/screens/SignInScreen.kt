package com.lifetracker.mobile.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifetracker.mobile.R
import com.lifetracker.mobile.ui.viewmodel.SignInViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun SignInScreen(
    vm: SignInViewModel = koinViewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                colors.primary.copy(alpha = 0.14f),
                                colors.background,
                                colors.secondary.copy(alpha = 0.10f),
                            ),
                    ),
                )
                .padding(horizontal = 28.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            HeroEmblem()

            Spacer(Modifier.height(4.dp))

            GradientTitle(text = "LifeTracker")

            Text(
                text = "Your hero, quests and gold — synced across devices.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(8.dp))

            FeatureHighlights()

            Spacer(Modifier.height(12.dp))

            GoogleSignInButton(
                isLoading = state.isSigningIn,
                isDark = isDark,
                onClick = { vm.startSignIn() },
            )

            AnimatedVisibility(
                visible = state.error != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                ErrorCard(message = state.error.orEmpty())
            }
        }
    }
}

@Composable
private fun HeroEmblem() {
    val colors = MaterialTheme.colorScheme
    val infinite = rememberInfiniteTransition(label = "emblemGlow")
    val glow by infinite.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.65f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 2200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "glowAlpha",
    )

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier =
                Modifier
                    .size(148.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors =
                                listOf(
                                    colors.primary.copy(alpha = glow * 0.55f),
                                    Color.Transparent,
                                ),
                        ),
                    ),
        )
        Box(
            modifier =
                Modifier
                    .size(108.dp)
                    .shadow(elevation = 18.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors =
                                listOf(
                                    colors.primary,
                                    colors.secondary,
                                ),
                        ),
                    )
                    .border(
                        width = 1.dp,
                        brush =
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.35f),
                                    Color.White.copy(alpha = 0.05f),
                                ),
                            ),
                        shape = CircleShape,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier.size(56.dp),
            )
        }
    }
}

@Composable
private fun GradientTitle(text: String) {
    val colors = MaterialTheme.colorScheme
    val gradient =
        Brush.linearGradient(
            colors = listOf(colors.primary, colors.secondary),
        )
    Text(
        text = text,
        style =
            MaterialTheme.typography.displaySmall.copy(
                brush = gradient,
                fontWeight = FontWeight.Bold,
            ),
    )
}

@Composable
private fun FeatureHighlights() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        FeatureHighlight(
            icon = Icons.Filled.Shield,
            label = "Keep progress",
        )
        FeatureHighlight(
            icon = Icons.Filled.CloudSync,
            label = "Sync devices",
        )
        FeatureHighlight(
            icon = Icons.Filled.EmojiEvents,
            label = "Never lose streaks",
        )
    }
}

@Composable
private fun FeatureHighlight(
    icon: ImageVector,
    label: String,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.width(96.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun GoogleSignInButton(
    isLoading: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val containerColor = if (isDark) Color(0xFF131314) else Color.White
    val contentColor = if (isDark) Color(0xFFE8EAED) else Color(0xFF1F1F1F)
    val disabledContainerColor =
        if (isDark) Color(0xFF131314).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.75f)
    val disabledContentColor = contentColor.copy(alpha = 0.6f)
    val borderColor =
        if (isDark) colors.primary.copy(alpha = 0.55f) else Color(0xFFDADCE0)

    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(elevation = if (isDark) 8.dp else 4.dp, shape = RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor,
                disabledContainerColor = disabledContainerColor,
                disabledContentColor = disabledContentColor,
            ),
        border = BorderStroke(1.dp, borderColor),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Crossfade(
            targetState = isLoading,
            animationSpec = tween(durationMillis = 180),
            label = "googleButtonState",
        ) { loading ->
            if (loading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = contentColor,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Signing in…",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_google_logo),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Sign in with Google",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    val colors = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.errorContainer.copy(alpha = 0.45f))
                .border(
                    width = 1.dp,
                    color = colors.error.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.ErrorOutline,
            contentDescription = null,
            tint = colors.error,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = message,
            color = colors.onErrorContainer,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
