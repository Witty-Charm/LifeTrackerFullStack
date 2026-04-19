package com.lifetracker.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifetracker.mobile.ui.model.HeroUi
import com.lifetracker.mobile.ui.theme.GoldYellow
import com.lifetracker.mobile.ui.theme.HealthRed
import com.lifetracker.mobile.ui.theme.HeroTileGradientEnd
import com.lifetracker.mobile.ui.theme.HeroTileGradientStart

@Composable
fun HeroSection(
    modifier: Modifier = Modifier,
    hero: HeroUi,
    onRespawn: () -> Unit,
    isRespawnLoading: Boolean = false,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(0.40f),
                horizontalAlignment = Alignment.Start,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(122.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                brush =
                                    Brush.verticalGradient(
                                        colors =
                                            listOf(
                                                HeroTileGradientStart,
                                                HeroTileGradientEnd,
                                            ),
                                    ),
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    val initial =
                        hero.name
                            .firstOrNull()
                            ?.uppercaseChar()
                            ?.toString()
                            .orEmpty()
                    Text(
                        text = initial,
                        style =
                            MaterialTheme.typography.displaySmall.copy(
                                fontSize = 48.sp,
                                fontWeight = FontWeight.ExtraBold,
                            ),
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "Lvl ${hero.level}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = null,
                        tint = GoldYellow,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = hero.goldText,
                        style = MaterialTheme.typography.labelMedium,
                        color = GoldYellow,
                    )
                }

                if (hero.xpBoostPercent > 0 && hero.xpBoostTasksRemaining > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = "+${hero.xpBoostPercent}% XP",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "${hero.xpBoostTasksRemaining} tasks left",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Column(
                modifier =
                    Modifier
                        .weight(0.60f)
                        .padding(start = 0.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatBar(
                    icon = "❤️",
                    progress = hero.hpProgress,
                    fractionText = hero.hpText,
                    label = "Health",
                    barColor = HealthRed,
                )
                StatBar(
                    icon = "⭐",
                    progress = hero.xpProgress,
                    fractionText = hero.xpText,
                    label = "Experience",
                    barColor = GoldYellow,
                )
            }
        }

        if (showRespawnAction(isDead = hero.isDead)) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Button(
                    onClick = onRespawn,
                    enabled = !isRespawnLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    if (isRespawnLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onError,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(text = "Respawn", color = MaterialTheme.colorScheme.onError)
                    }
                }
            }
        }
    }
}

internal fun showRespawnAction(isDead: Boolean): Boolean = isDead

@Composable
private fun StatBar(
    icon: String,
    progress: Float,
    fractionText: String,
    label: String,
    barColor: Color,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = icon, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(6.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier =
                    Modifier
                        .weight(1f)
                        .height(10.dp)
                        .clip(CircleShape),
                color = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Butt,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = fractionText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
