package com.lifetracker.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.lifetracker.mobile.domain.model.CreateTaskParams
import com.lifetracker.mobile.domain.model.TaskDifficulty
import com.lifetracker.mobile.domain.model.TaskType
import com.lifetracker.mobile.ui.model.HeroScreenState
import com.lifetracker.mobile.ui.model.UiError
import com.lifetracker.mobile.ui.viewmodel.HeroViewModel
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskScreen(
    state: HeroScreenState,
    vm: HeroViewModel,
    navController: NavController
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TaskType.OneTime) }
    var selectedDifficulty by remember { mutableStateOf(TaskDifficulty.Easy) }
    var dueDate by remember { mutableStateOf<Instant?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState()

    var didSubmit by remember { mutableStateOf(false) }
    var submittedTaskCountBaseline by remember { mutableStateOf(0) }

    LaunchedEffect(state.tasks.size, didSubmit) {
        if (didSubmit && state.tasks.size > submittedTaskCountBaseline) {
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create task") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Task name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                maxLines = 3
            )

            Text(text = "Type", style = MaterialTheme.typography.titleSmall)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedType == TaskType.Habit,
                    onClick = { selectedType = TaskType.Habit },
                    label = { Text("Habbit") }
                )
                FilterChip(
                    selected = selectedType == TaskType.OneTime,
                    onClick = { selectedType = TaskType.OneTime },
                    label = { Text("OneTime") }
                )
            }

            Text(text = "Difficulty", style = MaterialTheme.typography.titleSmall)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    TaskDifficulty.Easy to "Easy",
                    TaskDifficulty.Medium to "Medium",
                    TaskDifficulty.Hard to "Hard",
                    TaskDifficulty.Epic to "Epic",
                ).forEach { (difficulty, label) ->
                    FilterChip(
                        selected = selectedDifficulty == difficulty,
                        onClick = { selectedDifficulty = difficulty },
                        label = { Text(label) }
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = { showDatePicker = true }) {
                    Text("Set date")
                }

                if (dueDate != null) {
                    Text(
                        text = dueDate.toString(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(onClick = { dueDate = null }) {
                        Text("Clear")
                    }
                }
            }

            val errorText = state.actionError?.let { error ->
                when (error) {
                    is UiError.HeroDead -> "Hero is dead"
                    is UiError.DailyLimitReached -> "Daily limit reached (${error.completions}/${error.max})"
                    is UiError.Validation -> error.fieldErrors.values.flatten().joinToString(". ")
                    is UiError.Network -> "Network error"
                    is UiError.Generic -> error.message
                }
            }

            if (errorText != null) {
                Text(
                    text = errorText,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    didSubmit = true
                    submittedTaskCountBaseline = state.tasks.size
                    vm.createTask(
                        CreateTaskParams(
                            title = title,
                            description = description.ifBlank { null },
                            type = selectedType,
                            difficulty = selectedDifficulty,
                            dueDate = dueDate
                        )
                    )
                },
                enabled = title.isNotBlank() && !state.isActionLoading,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Save")
            }
        }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val millis = datePickerState.selectedDateMillis
                            dueDate = millis?.let { Instant.fromEpochMilliseconds(it) }
                            showDatePicker = false
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}

