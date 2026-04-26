package com.lifetracker.mobile.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.lifetracker.mobile.domain.model.ChecklistItem
import com.lifetracker.mobile.domain.model.ReminderItem
import com.lifetracker.mobile.ui.components.CreateScreenFloatingFooter
import com.lifetracker.mobile.ui.components.CreateScreenTopBar
import com.lifetracker.mobile.ui.components.GameDatePickerDialog
import com.lifetracker.mobile.ui.components.GameTextField
import com.lifetracker.mobile.ui.model.CreateDailyFormState
import com.lifetracker.mobile.ui.model.RepeatFrequency
import com.lifetracker.mobile.ui.model.UiDifficulty
import com.lifetracker.mobile.ui.viewmodel.CreateDailyUiEvent
import com.lifetracker.mobile.ui.viewmodel.CreateDailyViewModel
import com.lifetracker.mobile.ui.components.GlassmorphismSnackbar
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateDailyScreen(
    state: CreateDailyFormState,
    vm: CreateDailyViewModel,
    navController: NavController,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var newReminderHour by remember { mutableIntStateOf(9) }
    var newReminderMinute by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val hazeState = rememberHazeState()

    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is CreateDailyUiEvent.Success -> {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("task_changed", true)
                    navController.popBackStack()
                }
            }
        }
    }

    LaunchedEffect(state.actionError) {
        state.actionError?.let {
            val msg =
                when (it) {
                    is com.lifetracker.mobile.ui.model.UiError.Network -> {
                        "Network error. Try again."
                    }

                    is com.lifetracker.mobile.ui.model.UiError.Validation -> {
                        it.fieldErrors.values
                            .flatten()
                            .joinToString(", ")
                    }

                    is com.lifetracker.mobile.ui.model.UiError.Generic -> {
                        it.message
                    }

                    else -> {
                        "Something went wrong."
                    }
                }
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { GlassmorphismSnackbar(it, hazeState) },
            )
        },
        topBar = {
            CreateScreenTopBar(
                title = if (state.isEditMode) "Edit Daily" else "Create Daily",
                onBack = { navController.popBackStack() },
            )
        },
        bottomBar = {
            CreateScreenFloatingFooter(
                actionLabel = if (state.isEditMode) "Save changes" else "Save",
                enabled = state.canSubmit,
                onClick = { vm.onSubmit() },
                isLoading = state.isSaving,
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .hazeSource(hazeState)
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            GameTextField(
                value = state.title,
                onValueChange = vm::onTitleChange,
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            GameTextField(
                value = state.description,
                onValueChange = vm::onDescriptionChange,
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
            )

            ChecklistSection(
                items = state.checklistItems,
                onAdd = vm::onChecklistAdd,
                onDelete = vm::onChecklistDelete,
                onTextChange = vm::onChecklistTextChange,
                onReorder = vm::onReorder,
            )

            Text("Difficulty", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    UiDifficulty.Easy to "Easy",
                    UiDifficulty.Medium to "Medium",
                    UiDifficulty.Hard to "Hard",
                    UiDifficulty.Epic to "Epic",
                ).forEach { (difficulty, label) ->
                    FilterChip(
                        selected = state.difficulty == difficulty,
                        onClick = { vm.onDifficultyChange(difficulty) },
                        label = { Text(label) },
                    )
                }
            }

            SchedulingSection(
                startDate = state.startDate,
                interval = state.interval,
                onDateClick = { showDatePicker = true },
                onIntervalChange = vm::onIntervalChange,
            )

            if (!state.isEditMode) {
                Text("Imported streak", style = MaterialTheme.typography.titleSmall)
                GameTextField(
                    value = state.initialStreak.toString(),
                    onValueChange = { vm.onInitialStreakChange(it.toIntOrNull() ?: 0) },
                    label = { Text("Initial streak") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            RemindersSection(
                reminders = state.reminders,
                onAdd = { showTimePicker = true },
                onDelete = vm::onReminderDelete,
            )
        }

        GameDatePickerDialog(
            visible = showDatePicker,
            hazeState = hazeState,
            initialDate =
                state.startDate
                    ?.toLocalDateTime(TimeZone.currentSystemDefault())
                    ?.date
                    ?: Clock.System.todayIn(TimeZone.currentSystemDefault()),
            onDateSelected = { date ->
                vm.onStartDateChange(date.atStartOfDayIn(TimeZone.currentSystemDefault()))
            },
            onDismiss = { showDatePicker = false },
        )

        val reminderInteractionSource = remember { MutableInteractionSource() }
        AnimatedVisibility(
            visible = showTimePicker,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            val dialogShape = RoundedCornerShape(24.dp)
            val surfaceColor = MaterialTheme.colorScheme.surface
            val isDark = isSystemInDarkTheme()
            val borderColor = if (isDark) Color.White else Color.Black
            val glassBg = if (isDark)
                surfaceColor.copy(alpha = 0.85f)
            else
                surfaceColor.copy(alpha = 0.90f)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(
                        interactionSource = reminderInteractionSource,
                        indication = null,
                    ) { showTimePicker = false },
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .clip(dialogShape)
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
                            shape = dialogShape,
                        )
                        .clickable(
                            interactionSource = reminderInteractionSource,
                            indication = null,
                            onClick = {},
                        )
                        .padding(24.dp),
                ) {
                    Text(
                        text = "New reminder",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GameTextField(
                            value = newReminderHour.toString(),
                            onValueChange = { newReminderHour = it.toIntOrNull()?.coerceIn(0, 23) ?: 0 },
                            label = { Text("Hour") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                        GameTextField(
                            value = newReminderMinute.toString(),
                            onValueChange = { newReminderMinute = it.toIntOrNull()?.coerceIn(0, 59) ?: 0 },
                            label = { Text("Minute") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text("Cancel")
                        }
                        TextButton(onClick = {
                            vm.onReminderAdd(newReminderHour, newReminderMinute)
                            showTimePicker = false
                        }) {
                            Text("OK")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChecklistSection(
    items: List<ChecklistItem>,
    onAdd: () -> Unit,
    onDelete: (String) -> Unit,
    onTextChange: (String, String) -> Unit,
    onReorder: (List<ChecklistItem>) -> Unit,
) {
    Text("Checklist", style = MaterialTheme.typography.titleSmall)
    if (items.isNotEmpty()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items.forEachIndexed { index, item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IconButton(onClick = { onDelete(item.id) }) {
                        Icon(Icons.Default.Close, contentDescription = "Remove")
                    }
                    GameTextField(
                        value = item.text,
                        onValueChange = { onTextChange(item.id, it) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    Column {
                        IconButton(
                            onClick = {
                                if (index > 0) {
                                    val list = items.toMutableList()
                                    list.add(index - 1, list.removeAt(index))
                                    onReorder(list)
                                }
                            },
                            modifier = Modifier.size(28.dp),
                            enabled = index > 0,
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "Move up", modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = {
                                if (index < items.lastIndex) {
                                    val list = items.toMutableList()
                                    list.add(index + 1, list.removeAt(index))
                                    onReorder(list)
                                }
                            },
                            modifier = Modifier.size(28.dp),
                            enabled = index < items.lastIndex,
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = "Move down", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
    TextButton(onClick = onAdd) { Text("+ New checklist entry") }
}

@Composable
private fun SchedulingSection(
    startDate: Instant?,
    interval: Int,
    onDateClick: () -> Unit,
    onIntervalChange: (Int) -> Unit,
) {
    Text("Scheduling", style = MaterialTheme.typography.titleSmall)
    Text("Start Date")
    TextButton(onClick = onDateClick) {
        Text(startDate?.toDisplayDate() ?: "Select date")
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GameTextField(
            value = RepeatFrequency.DAILY.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Repeats") },
            modifier = Modifier.weight(1.5f),
        )
        GameTextField(
            value = interval.toString(),
            onValueChange = { value -> onIntervalChange(value.toIntOrNull() ?: 1) },
            label = { Text("Every (${RepeatFrequency.DAILY.unitLabel})") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
        )
    }
    Text("Repeats DAILY every $interval ${RepeatFrequency.DAILY.unitLabel}")
}

@Composable
private fun RemindersSection(
    reminders: List<ReminderItem>,
    onAdd: () -> Unit,
    onDelete: (String) -> Unit,
) {
    Text("Reminders", style = MaterialTheme.typography.titleSmall)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        reminders.forEach { reminder ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("${reminder.hour.toString().padStart(2, '0')}:${reminder.minute.toString().padStart(2, '0')}")
                IconButton(onClick = { onDelete(reminder.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
    TextButton(onClick = onAdd) { Text("+ New reminder") }
}

private val mediumDateFormatter: DateTimeFormatter by lazy {
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())
}

private fun Instant.toDisplayDate(): String {
    val javaDate = this.toLocalDateTime(TimeZone.currentSystemDefault()).date.toJavaLocalDate()
    return mediumDateFormatter.format(javaDate)
}