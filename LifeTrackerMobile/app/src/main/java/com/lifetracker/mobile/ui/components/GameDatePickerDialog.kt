package com.lifetracker.mobile.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import kotlinx.datetime.*
import kotlinx.datetime.LocalDate
import kotlin.time.Clock

private fun LocalDate.firstOfMonth(): LocalDate = LocalDate(year, month, 1)
private fun LocalDate.isSameMonth(other: LocalDate) = year == other.year && month == other.month
private fun LocalDate.monthLabel(): String =
    month.name.lowercase().replaceFirstChar { it.uppercase() } + " $year"

@Composable
fun GameDatePickerDialog(
    visible: Boolean,
    hazeState: HazeState,
    initialDate: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    var displayMonth by remember { mutableStateOf(initialDate.firstOfMonth()) }
    var selectedDate by remember { mutableStateOf(initialDate) }
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val interactionSource = remember { MutableInteractionSource() }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                ) { onDismiss() },
            contentAlignment = Alignment.Center,
        ) {
            DialogSurface(
                hazeState = hazeState,
                interactionSource = interactionSource,
                displayMonth = displayMonth,
                selectedDate = selectedDate,
                today = today,
                onMonthChange = { displayMonth = displayMonth.plus(it, DateTimeUnit.MONTH) },
                onSelect = { selectedDate = it },
                onDismiss = onDismiss,
                onOk = {
                    onDateSelected(selectedDate)
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun DialogSurface(
    hazeState: HazeState,
    interactionSource: MutableInteractionSource,
    displayMonth: LocalDate,
    selectedDate: LocalDate,
    today: LocalDate,
    onMonthChange: (Int) -> Unit,
    onSelect: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    onOk: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    val surfaceColor = colorScheme.surface

    Box(
        modifier = Modifier
            .fillMaxWidth(0.88f)
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
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {}
            ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onMonthChange(-1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = colorScheme.onSurface)
                }
                Text(
                    text = displayMonth.monthLabel(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface
                )
                IconButton(onClick = { onMonthChange(1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = colorScheme.onSurface)
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth()) {
                listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su").forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            val cells = remember(displayMonth) {
                val startOffset = displayMonth.dayOfWeek.isoDayNumber - 1
                val gridStart = displayMonth.minus(startOffset, DateTimeUnit.DAY)
                (0 until 42).map { gridStart.plus(it, DateTimeUnit.DAY) }
            }

            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        DateCell(
                            date = date,
                            isSelected = date == selectedDate,
                            isToday = date == today,
                            isCurrentMonth = date.isSameMonth(displayMonth),
                            onSelect = onSelect,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onOk) {
                    Text("OK", color = colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun DateCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    isCurrentMonth: Boolean,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier,
) {
    val colors = colorScheme

    val interactionSource = remember { MutableInteractionSource() }
    val click = { onSelect(date) }

    val textColor = when {
        isSelected -> colors.onPrimary
        !isCurrentMonth -> colors.onSurfaceVariant.copy(alpha = 0.30f)
        isToday -> colors.primary
        else -> colors.onSurface
    }

    val backgroundModifier = when {
        isSelected -> Modifier.background(colors.primary)
        isToday -> Modifier.border(1.dp, colors.primary, CircleShape)
        else -> Modifier
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .then(backgroundModifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = click,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = date.day.toString(),
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
        )
    }
}