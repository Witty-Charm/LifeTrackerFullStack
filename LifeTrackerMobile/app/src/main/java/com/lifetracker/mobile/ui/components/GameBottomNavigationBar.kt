package com.lifetracker.mobile.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifetracker.mobile.ui.theme.PurpleAccent
import com.lifetracker.mobile.ui.theme.SurfaceDark
import com.lifetracker.mobile.ui.theme.TextSecondary
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.launch

enum class HomeTab(val label: String) {
    Habits("Habits"),
    Dailies("Dailies"),
    ToDos("To Do's"),
    Rewards("Rewards")
}

private data class TabIcons(val outlined: ImageVector, val filled: ImageVector)

private fun tabIcons(tab: HomeTab): TabIcons = when (tab) {
    HomeTab.Habits  -> TabIcons(Icons.Outlined.FitnessCenter,  Icons.Filled.FitnessCenter)
    HomeTab.Dailies -> TabIcons(Icons.Outlined.CalendarToday,  Icons.Filled.CalendarToday)
    HomeTab.ToDos   -> TabIcons(Icons.Outlined.CheckCircle,    Icons.Filled.CheckCircle)
    HomeTab.Rewards -> TabIcons(Icons.Outlined.EmojiEvents,    Icons.Filled.EmojiEvents)
}

@Composable
fun GameBottomNavigationBar(
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    val tabs = HomeTab.entries
    val selectedIndex = tabs.indexOf(selectedTab)
    val shape = RoundedCornerShape(percent = 50)
    val density = LocalDensity.current

    val indicatorX = remember { Animatable(0f) }
    val indicatorWidth = remember { Animatable(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(shape)
                .hazeEffect(state = hazeState) {
                    backgroundColor = SurfaceDark
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
                .padding(vertical = 5.dp, horizontal = 4.dp),
        ) {
            val tabWidthPx = with(density) { (maxWidth / tabs.size).toPx() }
            val vPadPx = with(density) { 1.dp.toPx() }
            val inset = with(density) { 5.dp.toPx() }

            LaunchedEffect(selectedIndex, tabWidthPx) {
                val targetX = selectedIndex * tabWidthPx + inset
                val targetW = tabWidthPx - inset * 2f
                if (indicatorWidth.value == 0f) {
                    indicatorX.snapTo(targetX)
                    indicatorWidth.snapTo(targetW)
                } else {
                    launch {
                        indicatorX.animateTo(
                            targetValue = targetX,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessLow,
                            ),
                        )
                    }
                    launch {
                        indicatorWidth.animateTo(
                            targetValue = targetW,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium,
                            ),
                        )
                    }
                }
            }

            val glassTop = PurpleAccent.copy(alpha = 0.28f)
            val glassBottom = PurpleAccent.copy(alpha = 0.08f)
            val specular = Color.White.copy(alpha = 0.32f)
            val borderGlass = Color.White.copy(alpha = 0.14f)
            val innerGlow = PurpleAccent.copy(alpha = 0.12f)

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawBehind {
                        if (indicatorWidth.value <= 0f) return@drawBehind

                        val iX = indicatorX.value
                        val iW = indicatorWidth.value
                        val iH = size.height
                        val radius = CornerRadius(iH / 2f)
                        val top = vPadPx
                        val bottom = iH - vPadPx

                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(glassTop, glassBottom),
                                startY = top,
                                endY = bottom,
                            ),
                            topLeft = Offset(iX, top),
                            size = Size(iW, bottom - top),
                            cornerRadius = radius,
                        )

                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, specular, Color.Transparent),
                                startX = iX + iW * 0.15f,
                                endX   = iX + iW * 0.85f,
                            ),
                            topLeft = Offset(iX + iW * 0.15f, top + 2.dp.toPx()),
                            size = Size(iW * 0.70f, 1.5.dp.toPx()),
                            cornerRadius = CornerRadius(1.dp.toPx()),
                        )

                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, innerGlow),
                                startY = bottom - 8.dp.toPx(),
                                endY   = bottom,
                            ),
                            topLeft = Offset(iX + 4.dp.toPx(), bottom - 7.dp.toPx()),
                            size = Size(iW - 8.dp.toPx(), 5.dp.toPx()),
                            cornerRadius = CornerRadius(2.dp.toPx()),
                        )

                        drawRoundRect(
                            color = borderGlass,
                            topLeft = Offset(iX, top),
                            size = Size(iW, bottom - top),
                            cornerRadius = radius,
                            style = Stroke(width = 1.dp.toPx()),
                        )
                    },
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                tabs.forEach { tab ->
                    GlassTabItem(
                        tab = tab,
                        isSelected = tab == selectedTab,
                        onSelect = { onTabSelected(tab) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassTabItem(
    tab: HomeTab,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val icons = remember(tab) { tabIcons(tab) }

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.84f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "pressScale_${tab.name}",
    )

    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "iconScale_${tab.name}",
    )

    val iconOffsetY by animateDpAsState(
        targetValue = if (isSelected) (-1).dp else 1.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "iconOffsetY_${tab.name}",
    )

    val labelAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(durationMillis = if (isSelected) 220 else 120),
        label = "labelAlpha_${tab.name}",
    )

    val labelScale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.6f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "labelScale_${tab.name}",
    )

    val iconColor = if (isSelected) PurpleAccent else TextSecondary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
        modifier = modifier
            .height(52.dp)
            .clipToBounds()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
            ) { onSelect() }
            .scale(pressScale)
            .padding(vertical = 6.dp),
    ) {
        Icon(
            imageVector = if (isSelected) icons.filled else icons.outlined,
            contentDescription = tab.label,
            tint = iconColor,
            modifier = Modifier
                .size(22.dp)
                .scale(iconScale)
                .offset(y = iconOffsetY),
        )

        Box(
            modifier = Modifier
                .height(if (isSelected) 16.dp else 0.dp)
                .graphicsLayer {
                    alpha = labelAlpha
                    scaleX = labelScale
                    scaleY = labelScale
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = tab.label,
                fontSize = 10.sp,
                color = iconColor,
                maxLines = 1,
            )
        }
    }
}
