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
import com.lifetracker.mobile.ui.model.HeroScreenState
import com.lifetracker.mobile.ui.mapper.toMessage
import com.lifetracker.mobile.ui.model.UiDifficulty
import com.lifetracker.mobile.ui.model.UiTaskType
import com.lifetracker.mobile.ui.viewmodel.HeroViewModel
import androidx.compose.ui.platform.LocalContext
import com.lifetracker.mobile.ui.model.isAnyActionLoading
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
    var selectedType by remember { mutableStateOf(UiTaskType.OneTime) }
    var selectedDifficulty by remember { mutableStateOf(UiDifficulty.Easy) }
    var dueDate by remember { mutableStateOf<Instant?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState()

    LaunchedEffect(Unit) {
        vm.clearError()
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
                    selected = selectedType == UiTaskType.Habit,
                    onClick = { selectedType = UiTaskType.Habit },
                    label = { Text("Habit") }
                )
                FilterChip(
                    selected = selectedType == UiTaskType.OneTime,
                    onClick = { selectedType = UiTaskType.OneTime },
                    label = { Text("One Time") }
                )
            }

            Text(text = "Difficulty", style = MaterialTheme.typography.titleSmall)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    UiDifficulty.Easy to "Easy",
                    UiDifficulty.Medium to "Medium",
                    UiDifficulty.Hard to "Hard",
                    UiDifficulty.Epic to "Epic",
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

            val context = LocalContext.current
            val errorText = state.actionError?.toMessage(context)

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
                    vm.createTask(
                        title = title,
                        description = description.ifBlank { null },
                        type = selectedType,
                        difficulty = selectedDifficulty,
                        dueDate = dueDate,
                    )
                },
                enabled = title.isNotBlank() && !state.isAnyActionLoading,
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

