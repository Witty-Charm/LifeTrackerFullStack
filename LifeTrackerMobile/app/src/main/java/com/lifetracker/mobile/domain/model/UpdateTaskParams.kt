package com.lifetracker.mobile.domain.model

import kotlin.time.Instant

data class UpdateTaskParams(
    val taskId: Int,
    val type: TaskType,
    val title: String,
    val description: String? = null,
    val difficulty: TaskDifficulty = TaskDifficulty.Easy,
    val habitPolarity: HabitPolarity = HabitPolarity.Both,
    val dueDate: Instant? = null,
    val repeatPattern: String? = null,
    val checklistJson: String? = null,
    val remindersJson: String? = null,
)
