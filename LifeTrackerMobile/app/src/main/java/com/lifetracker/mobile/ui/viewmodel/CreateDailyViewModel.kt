package com.lifetracker.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifetracker.mobile.core.reminder.ReminderScheduler
import com.lifetracker.mobile.core.serialization.JsonDefaults
import com.lifetracker.mobile.domain.model.ChecklistItem
import com.lifetracker.mobile.domain.model.CreateTaskParams
import com.lifetracker.mobile.domain.model.DomainResult
import com.lifetracker.mobile.domain.model.GameError
import com.lifetracker.mobile.domain.model.ReminderItem
import com.lifetracker.mobile.domain.model.TaskType
import com.lifetracker.mobile.domain.model.fold
import com.lifetracker.mobile.domain.usecase.task.TaskUseCases
import com.lifetracker.mobile.ui.mapper.toDomain
import com.lifetracker.mobile.ui.model.CreateDailyFormState
import com.lifetracker.mobile.ui.model.RepeatFrequency
import com.lifetracker.mobile.ui.model.UiDifficulty
import com.lifetracker.mobile.ui.mapper.toUiError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Instant
import timber.log.Timber

sealed interface CreateDailyUiEvent {
    data class Success(val type: TaskType) : CreateDailyUiEvent
}

class CreateDailyViewModel(
    private val heroId: Int,
    private val taskUseCases: TaskUseCases,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    private val _state = MutableStateFlow(CreateDailyFormState())
    val state: StateFlow<CreateDailyFormState> = _state.asStateFlow()

    private val _events = Channel<CreateDailyUiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onTitleChange(value: String) =
        _state.update { it.copy(title = value) }

    fun onDescriptionChange(value: String) =
        _state.update { it.copy(description = value) }

    fun onDifficultyChange(value: UiDifficulty) =
        _state.update { it.copy(difficulty = value) }

    fun onStartDateChange(value: Instant?) =
        _state.update { it.copy(startDate = value) }

    fun onFrequencyChange(value: RepeatFrequency) =
        _state.update { it.copy(frequency = value) }

    fun onIntervalChange(value: Int) =
        _state.update { it.copy(interval = value, intervalError = value < 1) }

    fun onInitialStreakChange(value: Int) =
        _state.update { it.copy(initialStreak = value) }

    fun onChecklistAdd() {
        val newItem = ChecklistItem(
            id = Random.nextInt(0, 0xFFFF).toString(16),
            text = "",
        )
        _state.update { it.copy(checklistItems = it.checklistItems + newItem) }
    }

    fun onChecklistDelete(id: String) =
        _state.update { it.copy(checklistItems = it.checklistItems.filterNot { item -> item.id == id }) }

    fun onChecklistTextChange(id: String, text: String) =
        _state.update {
            it.copy(checklistItems = it.checklistItems.map { item ->
                if (item.id == id) item.copy(text = text) else item
            })
        }

    fun onReorder(items: List<ChecklistItem>) =
        _state.update { it.copy(checklistItems = items) }

    fun onReminderAdd(hour: Int, minute: Int) {
        val newItem = ReminderItem(
            id = Random.nextInt(0, 0xFFFF).toString(16),
            hour = hour,
            minute = minute,
        )
        _state.update { it.copy(reminders = it.reminders + newItem) }
    }

    fun onReminderDelete(id: String) =
        _state.update { it.copy(reminders = it.reminders.filterNot { item -> item.id == id }) }

    fun onSubmit() {
        val s = _state.value
        if (!s.canSubmit) return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, actionError = null) }
            try {
                submit(s)
            } finally {
                _state.update { it.copy(isSaving = false) }
            }
        }
    }

    private suspend fun submit(s: CreateDailyFormState) {
        val repeatPattern = "${s.frequency.name}:${s.interval.coerceAtLeast(1)}"

        val checklistJson = s.checklistItems
            .takeIf { it.isNotEmpty() }
            ?.let { JsonDefaults.encodeToString(it) }

        val remindersJson = s.reminders
            .takeIf { it.isNotEmpty() }
            ?.let { JsonDefaults.encodeToString(it) }

        val params = CreateTaskParams(
            heroId = heroId,
            title = s.title,
            description = s.description.ifBlank { null },
            type = TaskType.Daily,
            difficulty = s.difficulty.toDomain(),
            dueDate = s.startDate,
            repeatPattern = repeatPattern,
            initialStreak = s.initialStreak,
            checklistJson = checklistJson,
            remindersJson = remindersJson,
        )

        safeCall { taskUseCases.createTask(params) }
            .fold(
                onSuccess = { task ->
                    if (!remindersJson.isNullOrBlank()) {
                        reminderScheduler.schedule(task.id, task.title, remindersJson, repeatPattern)
                    }
                    _events.send(CreateDailyUiEvent.Success(TaskType.Daily))
                },
                onFailure = { error ->
                    _state.update { it.copy(actionError = error.toUiError()) }
                },
            )
    }

    private suspend fun <T> safeCall(block: suspend () -> DomainResult<T>): DomainResult<T> =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Unexpected exception in CreateDailyViewModel")
            DomainResult.Failure(GameError.Unknown(e.message ?: "Unexpected error"))
        }
}
