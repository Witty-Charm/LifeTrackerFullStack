package com.lifetracker.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifetracker.mobile.ui.model.HeroUi
import com.lifetracker.mobile.ui.theme.CardBackground
import com.lifetracker.mobile.ui.theme.CardBorder
import com.lifetracker.mobile.ui.theme.GoldYellow
import com.lifetracker.mobile.ui.theme.HealthRed
import com.lifetracker.mobile.ui.theme.HeroTileGradientEnd
import com.lifetracker.mobile.ui.theme.HeroTileGradientStart
import com.lifetracker.mobile.ui.theme.OnGoldText
import com.lifetracker.mobile.ui.theme.PurpleAccent
import com.lifetracker.mobile.ui.theme.TextPrimary
import com.lifetracker.mobile.ui.theme.TextSecondary
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun HeroSection(
    hero: HeroUi,
    onHeal: () -> Unit,
    onRespawn: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(0.45f),
                horizontalAlignment = Alignment.Start
            ) {
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    HeroTileGradientStart,
                                    HeroTileGradientEnd
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val initial = hero.name.firstOrNull()?.uppercaseChar()?.toString().orEmpty()
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontSize = 48.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Lvl ${hero.level}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = null,
                        tint = GoldYellow,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = hero.goldText,
                        style = MaterialTheme.typography.labelMedium,
                        color = GoldYellow
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(0.55f)
                    .padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatBar(
                    icon = "❤️",
                    progress = hero.hpProgress,
                    fractionText = hero.hpText,
                    label = "Health",
                    barColor = HealthRed
                )
                StatBar(
                    icon = "⭐",
                    progress = hero.xpProgress,
                    fractionText = hero.xpText,
                    label = "Experience",
                    barColor = GoldYellow
                )
            }
        }

        if (hero.isDead || hero.isInRecovery) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (hero.isDead) {
                    Button(
                        onClick = onRespawn,
                        colors = ButtonDefaults.buttonColors(containerColor = HealthRed)
                    ) {
                        Text(text = "Respawn", color = TextPrimary)
                    }
                } else if (hero.isInRecovery) {
                    Button(
                        onClick = onHeal,
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
                    ) {
                        Text(text = "Heal", color = TextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBar(
    icon: String,
    progress: Float,
    fractionText: String,
    label: String,
    barColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.width(6.dp))
        Box(
            modifier = Modifier.weight(1f)
        ) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(CircleShape),
                color = barColor,
                trackColor = CardBorder
            )
            Text(
                text = fractionText,
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = TextSecondary
        )
    }
}

