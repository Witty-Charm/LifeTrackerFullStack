package com.lifetracker.mobile.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.launch

enum class HomeTab(val label: String) {
    Habits("Habits"),
    Dailies("Dailies"),
    ToDos("To Do's"),
    Shop("Shop")
}

private data class TabIcons(val outlined: ImageVector, val filled: ImageVector)

private fun tabIcons(tab: HomeTab): TabIcons = when (tab) {
    HomeTab.Habits  -> TabIcons(Icons.Outlined.FitnessCenter,  Icons.Filled.FitnessCenter)
    HomeTab.Dailies -> TabIcons(Icons.Outlined.CalendarToday,  Icons.Filled.CalendarToday)
    HomeTab.ToDos   -> TabIcons(Icons.Outlined.CheckCircle,    Icons.Filled.CheckCircle)
    HomeTab.Shop    -> TabIcons(Icons.Outlined.ShoppingCart,   Icons.Filled.ShoppingCart)
}

@Composable
fun GameBottomNavigationBar(
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    onAddClick: () -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    val tabs = HomeTab.entries
    val slotCount = 5
    val tabSlots = listOf(0, 1, 3, 4)
    val selectedIndex = tabs.indexOf(selectedTab)
    val selectedSlotIndex = tabSlots.getOrElse(selectedIndex) { 0 }
    val density = LocalDensity.current
    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outline

    val indicatorX = remember { Animatable(0f) }
    val indicatorWidth = remember { Animatable(0f) }
    val fabSize = 58.dp
    val fabRadius = fabSize / 2
    val fabYOffset = (-40).dp
    val barPadY = 5.dp
    val barPadX = 4.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .hazeEffect(state = hazeState) {
                    backgroundColor = surfaceColor
                    blurRadius = 28.dp
                    noiseFactor = 0f
                }
                .drawWithCache {
                    val cornerRadius = size.height / 2f
                    val baseRect = Rect(Offset.Zero, size)
                    val basePath = Path().apply {
                        addRoundRect(RoundRect(baseRect, cornerRadius, cornerRadius))
                    }
                    val cutoutRadiusPx = with(density) { (fabRadius + 8.dp).toPx() }
                    val cutoutCenter = Offset(
                        x = size.width / 2f,
                        y = -cutoutRadiusPx * 0.38f,
                    )
                    val cutoutPath = Path().apply {
                        addOval(
                            Rect(
                                center = cutoutCenter,
                                radius = cutoutRadiusPx,
                            ),
                        )
                    }
                    val compositePath = Path.combine(
                        PathOperation.Difference,
                        basePath,
                        cutoutPath,
                    )

                    onDrawWithContent {
                        clipPath(compositePath) {
                            this@onDrawWithContent.drawContent()
                        }
                        drawPath(
                            path = compositePath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    outlineColor.copy(alpha = 0.30f),
                                    outlineColor.copy(alpha = 0.08f),
                                ),
                            ),
                            style = Stroke(width = 0.5.dp.toPx()),
                        )
                    }
                }
                .padding(vertical = barPadY, horizontal = barPadX),
        ) {
            val tabWidthPx = with(density) { (maxWidth / slotCount).toPx() }
            val vPadPx = with(density) { 1.dp.toPx() }
            val inset = with(density) { 2.dp.toPx() }

            LaunchedEffect(selectedSlotIndex, tabWidthPx) {
                val targetX = selectedSlotIndex * tabWidthPx + inset
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

            val glassTop = primaryColor.copy(alpha = 0.28f)
            val glassBottom = primaryColor.copy(alpha = 0.08f)
            val specular = Color.White.copy(alpha = 0.32f)
            val borderGlass = Color.White.copy(alpha = 0.14f)
            val innerGlow = primaryColor.copy(alpha = 0.12f)

            Box(modifier = Modifier.matchParentSize())

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .zIndex(1f)
                    .drawBehind {
                        if (indicatorWidth.value <= 0f) return@drawBehind

                        val iX = indicatorX.value
                        val iW = indicatorWidth.value
                        val iH = size.height
                        val cornerRadiusPx: Float = iH / 2f
                        val radius = CornerRadius(cornerRadiusPx)
                        val top = vPadPx
                        val bottom = iH - vPadPx

                        val indicatorPath = Path().apply {
                            addRoundRect(
                                RoundRect(
                                    rect = Rect(
                                        offset = Offset(iX, top),
                                        size = Size(iW, bottom - top),
                                    ),
                                    cornerRadius = radius,
                                )
                            )
                        }

                        clipPath(indicatorPath) {
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
                        }

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
                tabs.take(2).forEach { tab ->
                    val onSelect = remember(tab) { { onTabSelected(tab) } }
                    GlassTabItem(
                        tab = tab,
                        isSelected = tab == selectedTab,
                        onSelect = onSelect,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                tabs.drop(2).forEach { tab ->
                    val onSelect = remember(tab) { { onTabSelected(tab) } }
                    GlassTabItem(
                        tab = tab,
                        isSelected = tab == selectedTab,
                        onSelect = onSelect,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        GlassAddFab(
            onClick = onAddClick,
            fabSize = fabSize,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = fabYOffset)
                .zIndex(2f),
        )
    }
}

@Composable
private fun GlassAddFab(
    onClick: () -> Unit,
    fabSize: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val primaryColor = MaterialTheme.colorScheme.primary

    val pressScaleState = animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "addFabPressScale",
    )

    Box(
        modifier = modifier
            .size(fabSize)
            .graphicsLayer {
                val s = pressScaleState.value
                scaleX = s
                scaleY = s
            }
            .drawBehind {
                val radius = size.minDimension / 2f

                drawCircle(
                    color = primaryColor.copy(alpha = 0.85f),
                    radius = radius,
                    center = center,
                )

                drawCircle(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.25f),
                            Color.Transparent,
                        ),
                        startY = 0f,
                        endY = size.height,
                    ),
                    radius = radius,
                    center = center,
                )

                drawCircle(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.4f),
                            Color.White.copy(alpha = 0.4f),
                            Color.Transparent,
                        ),
                    ),
                    radius = radius - 0.75.dp.toPx(),
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx()),
                )
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
            ) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "Add task",
            tint = Color.White,
            modifier = Modifier.size(28.dp),
        )
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

    val pressScaleState = animateFloatAsState(
        targetValue = if (isPressed) 0.84f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "pressScale_${tab.name}",
    )

    val iconScaleState = animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "iconScale_${tab.name}",
    )

    val iconOffsetYState = animateFloatAsState(
        targetValue = if (isSelected) -1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "iconOffsetY_${tab.name}",
    )

    val density = LocalDensity.current

    val labelAlphaState = animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(durationMillis = if (isSelected) 220 else 120),
        label = "labelAlpha_${tab.name}",
    )

    val labelScaleState = animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.6f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "labelScale_${tab.name}",
    )

    val iconColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

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
            .graphicsLayer {
                val s = pressScaleState.value
                scaleX = s
                scaleY = s
            }
            .padding(vertical = 6.dp),
    ) {
        Icon(
            imageVector = if (isSelected) icons.filled else icons.outlined,
            contentDescription = tab.label,
            tint = iconColor,
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer {
                    val s = iconScaleState.value
                    scaleX = s
                    scaleY = s
                    translationY = iconOffsetYState.value * density.density
                },
        )

        Box(
            modifier = Modifier
                .height(if (isSelected) 16.dp else 0.dp)
                .graphicsLayer {
                    alpha = labelAlphaState.value
                    scaleX = labelScaleState.value
                    scaleY = labelScaleState.value
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
