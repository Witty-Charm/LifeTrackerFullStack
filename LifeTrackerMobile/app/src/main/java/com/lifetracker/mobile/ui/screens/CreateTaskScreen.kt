package com.lifetracker.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.lifetracker.mobile.domain.model.HabitPolarity
import com.lifetracker.mobile.domain.model.HabitResetPeriod
import com.lifetracker.mobile.domain.model.TaskType
import com.lifetracker.mobile.domain.model.habitResetPeriod
import com.lifetracker.mobile.ui.components.CreateScreenFloatingFooter
import com.lifetracker.mobile.ui.components.CreateScreenTopBar
import com.lifetracker.mobile.ui.components.GameDatePickerDialog
import com.lifetracker.mobile.ui.components.GameTextField
import com.lifetracker.mobile.ui.mapper.toMessage
import com.lifetracker.mobile.ui.mapper.toUi
import com.lifetracker.mobile.ui.model.HeroScreenState
import com.lifetracker.mobile.ui.model.UiDifficulty
import com.lifetracker.mobile.ui.model.UiTaskType
import com.lifetracker.mobile.ui.model.isAnyActionLoading
import com.lifetracker.mobile.ui.viewmodel.HeroViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskScreen(
    state: HeroScreenState,
    vm: HeroViewModel,
    navController: NavController,
    initialType: UiTaskType = UiTaskType.OneTime,
    lockTypeSelection: Boolean = false,
    editingTaskId: Int? = null,
) {
    val isEditMode = editingTaskId != null
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedType by remember(initialType) {
        mutableStateOf(initialType.takeIf { it == UiTaskType.Habit || it == UiTaskType.OneTime } ?: UiTaskType.OneTime)
    }
    var selectedDifficulty by remember { mutableStateOf(UiDifficulty.Easy) }
    var selectedHabitPolarity by remember(selectedType) { mutableStateOf(defaultHabitPolarity(selectedType)) }
    var selectedHabitResetPeriod by remember(selectedType) { mutableStateOf(HabitResetPeriod.Default) }
    var dueDate by remember { mutableStateOf<Instant?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var hasLoadedForEdit by remember(editingTaskId) { mutableStateOf(false) }
    val context = LocalContext.current
    val hazeState = remember { HazeState() }
    val screenTitle =
        when {
            isEditMode && selectedType == UiTaskType.Habit -> "Edit habit"
            isEditMode -> "Edit to do"
            !lockTypeSelection -> "Create task"
            selectedType == UiTaskType.Habit -> "Create habit"
            else -> "Create to do"
        }
    val actionLabel = if (isEditMode) "Save changes" else "Save"

    LaunchedEffect(Unit) {
        vm.clearError()
    }

    LaunchedEffect(editingTaskId) {
        if (editingTaskId == null) return@LaunchedEffect
        val inMemory = state.tasks.firstOrNull { it.id == editingTaskId }
        inMemory?.let {
            selectedType = it.type
            title = it.title
            description = it.description
            selectedHabitPolarity = it.habitPolarity
        }
        val task = vm.loadTaskForEdit(editingTaskId)
        if (task == null && inMemory == null) {
            navController.popBackStack()
            return@LaunchedEffect
        }
        task?.let {
            selectedType = it.type.toUi()
            title = it.title
            description = it.description
            selectedDifficulty = it.difficulty.toUi()
            selectedHabitPolarity = it.habitPolarity
            selectedHabitResetPeriod = it.habitResetPeriod ?: HabitResetPeriod.Default
            dueDate = if (it.type == TaskType.Habit) null else it.dueDate
        }
        hasLoadedForEdit = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                CreateScreenTopBar(
                    title = screenTitle,
                    onBack = { navController.popBackStack() },
                )
            },
            bottomBar = {
                CreateScreenFloatingFooter(
                    actionLabel = actionLabel,
                    enabled =
                        title.isNotBlank() &&
                            !state.isAnyActionLoading &&
                            (!isEditMode || hasLoadedForEdit),
                    onClick = {
                        val effectiveDueDate = if (selectedType == UiTaskType.Habit) null else dueDate
                        if (editingTaskId != null) {
                            vm.updateTask(
                                taskId = editingTaskId,
                                title = title,
                                description = description.ifBlank { null },
                                type = selectedType,
                                difficulty = selectedDifficulty,
                                dueDate = effectiveDueDate,
                                habitPolarity = selectedHabitPolarity,
                                habitResetPeriod = selectedHabitResetPeriod,
                            )
                        } else {
                            vm.createTask(
                                title = title,
                                description = description.ifBlank { null },
                                type = selectedType,
                                difficulty = selectedDifficulty,
                                dueDate = effectiveDueDate,
                                habitPolarity = selectedHabitPolarity,
                                habitResetPeriod = selectedHabitResetPeriod,
                            )
                        }
                    },
                    isLoading = state.isAnyActionLoading,
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
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(taskNameLabel(selectedType, lockTypeSelection)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                GameTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    maxLines = 3,
                )

                if (!lockTypeSelection) {
                    Text(text = "Type", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedType == UiTaskType.Habit,
                            onClick = {
                                selectedType = UiTaskType.Habit
                                selectedHabitPolarity = defaultHabitPolarity(UiTaskType.Habit)
                            },
                            label = { Text("Habit") },
                        )
                        FilterChip(
                            selected = selectedType == UiTaskType.OneTime,
                            onClick = {
                                selectedType = UiTaskType.OneTime
                                selectedHabitPolarity = defaultHabitPolarity(UiTaskType.OneTime)
                            },
                            label = { Text("One Time") },
                        )
                    }
                }

                if (shouldShowHabitPolarity(selectedType)) {
                    Text(text = "Polarity", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedHabitPolarity == HabitPolarity.Positive,
                            onClick = { selectedHabitPolarity = HabitPolarity.Positive },
                            label = { Text("Positive") },
                        )
                        FilterChip(
                            selected = selectedHabitPolarity == HabitPolarity.Negative,
                            onClick = { selectedHabitPolarity = HabitPolarity.Negative },
                            label = { Text("Negative") },
                        )
                        FilterChip(
                            selected = selectedHabitPolarity == HabitPolarity.Both,
                            onClick = { selectedHabitPolarity = HabitPolarity.Both },
                            label = { Text("Both") },
                        )
                    }
                }

                Text(text = "Difficulty", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        UiDifficulty.Easy to "Easy",
                        UiDifficulty.Medium to "Medium",
                        UiDifficulty.Hard to "Hard",
                        UiDifficulty.Epic to "Epic",
                    ).forEach { (difficulty, label) ->
                        FilterChip(
                            selected = selectedDifficulty == difficulty,
                            onClick = { selectedDifficulty = difficulty },
                            label = { Text(label) },
                        )
                    }
                }

                if (selectedType == UiTaskType.Habit) {
                    Text(text = "Reset counter", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HabitResetPeriod.entries.forEach { period ->
                            FilterChip(
                                selected = selectedHabitResetPeriod == period,
                                onClick = { selectedHabitResetPeriod = period },
                                label = { Text(period.label) },
                            )
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TextButton(onClick = { showDatePicker = true }) {
                            Text("Set date")
                        }

                        if (dueDate != null) {
                            Text(
                                text = dueDate.toDisplayDate(),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            TextButton(onClick = { dueDate = null }) {
                                Text("Clear")
                            }
                        }
                    }
                }

                val errorText = state.actionError?.toMessage(context)
                if (errorText != null) {
                    Text(
                        text = errorText,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        GameDatePickerDialog(
            visible = showDatePicker,
            hazeState = hazeState,
            initialDate =
                dueDate
                    ?.toLocalDateTime(TimeZone.currentSystemDefault())
                    ?.date
                    ?: Clock.System.todayIn(TimeZone.currentSystemDefault()),
            onDateSelected = { date ->
                dueDate = date.atStartOfDayIn(TimeZone.currentSystemDefault())
            },
            onDismiss = { showDatePicker = false },
        )
    }
}

internal fun taskNameLabel(
    selectedType: UiTaskType,
    lockTypeSelection: Boolean,
): String =
    when {
        !lockTypeSelection -> "Task name"
        selectedType == UiTaskType.Habit -> "Habit name"
        else -> "To do name"
    }

internal fun shouldShowHabitPolarity(selectedType: UiTaskType): Boolean = selectedType == UiTaskType.Habit

internal fun defaultHabitPolarity(selectedType: UiTaskType): HabitPolarity =
    if (selectedType == UiTaskType.Habit) HabitPolarity.Both else HabitPolarity.Both

private val DisplayDateFormat =
    LocalDate.Format {
        day()
        char(' ')
        monthName(MonthNames.ENGLISH_ABBREVIATED)
        char(' ')
        year()
    }

private fun Instant?.toDisplayDate(): String =
    this
        ?.toLocalDateTime(TimeZone.currentSystemDefault())
        ?.date
        ?.format(DisplayDateFormat)
        ?: ""
