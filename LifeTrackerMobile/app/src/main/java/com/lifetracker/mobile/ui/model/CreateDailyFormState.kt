package com.lifetracker.mobile.ui.model

import com.lifetracker.mobile.domain.model.ChecklistItem
import com.lifetracker.mobile.domain.model.ReminderItem
import kotlin.time.Instant

data class CreateDailyFormState(
    val title: String = "",
    val description: String = "",
    val difficulty: UiDifficulty = UiDifficulty.Easy,
    val startDate: Instant? = Instant.fromEpochMilliseconds(System.currentTimeMillis()),
    val frequency: RepeatFrequency = RepeatFrequency.DAILY,
    val interval: Int = 1,
    val intervalError: Boolean = false,
    val initialStreak: Int = 0,
    val checklistItems: List<ChecklistItem> = emptyList(),
    val reminders: List<ReminderItem> = emptyList(),
    val isSaving: Boolean = false,
    val actionError: UiError? = null,
) {
    val canSubmit: Boolean get() = title.isNotBlank() && !intervalError && !isSaving
}
