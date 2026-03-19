package com.lifetracker.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.lifetracker.mobile.domain.model.ChecklistItem
import com.lifetracker.mobile.domain.model.ReminderItem
import com.lifetracker.mobile.ui.model.CreateDailyFormState
import com.lifetracker.mobile.ui.model.RepeatFrequency
import com.lifetracker.mobile.ui.model.UiDifficulty
import com.lifetracker.mobile.ui.viewmodel.CreateDailyUiEvent
import com.lifetracker.mobile.ui.viewmodel.CreateDailyViewModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
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
    var expandedFrequency by remember { mutableStateOf(false) }
    var newReminderHour by remember { mutableIntStateOf(9) }
    var newReminderMinute by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is CreateDailyUiEvent.Success -> {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("daily_created", true)
                    navController.popBackStack()
                }
            }
        }
    }

    LaunchedEffect(state.actionError) {
        state.actionError?.let {
            val msg = when (it) {
                is com.lifetracker.mobile.ui.model.UiError.Network -> "Network error. Try again."
                is com.lifetracker.mobile.ui.model.UiError.Validation ->
                    it.fieldErrors.values.flatten().joinToString(", ")
                is com.lifetracker.mobile.ui.model.UiError.Generic -> it.message
                else -> "Something went wrong."
            }
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Create daily") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { vm.onSubmit() },
                        enabled = state.canSubmit,
                    ) {
                        Text("Save")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = state.title,
                onValueChange = vm::onTitleChange,
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
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
                        label = { Text(label) }
                    )
                }
            }

            SchedulingSection(
                startDate = state.startDate,
                frequency = state.frequency,
                interval = state.interval,
                onDateClick = { showDatePicker = true },
                onFrequencyChange = vm::onFrequencyChange,
                onIntervalChange = vm::onIntervalChange,
                expanded = expandedFrequency,
                onExpandedChange = { expandedFrequency = it },
            )

            Text("Adjust Streak", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = state.initialStreak.toString(),
                onValueChange = { vm.onInitialStreakChange(it.toIntOrNull() ?: 0) },
                label = { Text("Streak") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            RemindersSection(
                reminders = state.reminders,
                onAdd = { showTimePicker = true },
                onDelete = vm::onReminderDelete,
            )
        }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState()
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val millis = datePickerState.selectedDateMillis
                        vm.onStartDateChange(millis?.let { Instant.fromEpochMilliseconds(it) })
                        showDatePicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                }
            ) { DatePicker(state = datePickerState) }
        }

        if (showTimePicker) {
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                title = { Text("New reminder") },
                text = {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = newReminderHour.toString(),
                            onValueChange = { newReminderHour = it.toIntOrNull()?.coerceIn(0, 23) ?: 0 },
                            label = { Text("Hour") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = newReminderMinute.toString(),
                            onValueChange = { newReminderMinute = it.toIntOrNull()?.coerceIn(0, 59) ?: 0 },
                            label = { Text("Minute") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        vm.onReminderAdd(newReminderHour, newReminderMinute)
                        showTimePicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                }
            )
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
                    OutlinedTextField(
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
                                    // Экран сам вычисляет новый порядок и передаёт готовый список.
                                    // ViewModel не знает про индексы — только про результат.
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SchedulingSection(
    startDate: Instant?,
    frequency: RepeatFrequency,
    interval: Int,
    onDateClick: () -> Unit,
    onFrequencyChange: (RepeatFrequency) -> Unit,
    onIntervalChange: (Int) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
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
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = onExpandedChange,
            modifier = Modifier.weight(1.5f),
        ) {
            OutlinedTextField(
                value = frequency.label,
                onValueChange = {},
                readOnly = true,
                label = { Text("Repeats") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor(androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) }
            ) {
                RepeatFrequency.entries.forEach { option ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            onFrequencyChange(option)
                            onExpandedChange(false)
                        }
                    )
                }
            }
        }
        OutlinedTextField(
            value = interval.toString(),
            onValueChange = { value -> onIntervalChange(value.toIntOrNull() ?: 1) },
            label = { Text("Every (${frequency.unitLabel})") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
        )
    }
    Text("Repeats ${frequency.label.uppercase(Locale.getDefault())} every $interval ${frequency.unitLabel}")
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

private fun Instant.toDisplayDate(): String {
    val javaDate = this.toLocalDateTime(TimeZone.currentSystemDefault()).date.toJavaLocalDate()
    return DateTimeFormatter
        .ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(Locale.getDefault())
        .format(javaDate)
}
