package com.lifetracker.mobile.domain.model

import kotlin.time.Instant

data class CreateTaskParams(
    val heroId: Int,
    val title: String,
    val description: String? = null,
    val type: TaskType = TaskType.OneTime,
    val difficulty: TaskDifficulty = TaskDifficulty.Easy,
    val habitPolarity: HabitPolarity = HabitPolarity.Both,
    val habitResetPeriod: HabitResetPeriod = HabitResetPeriod.Default,
    val dueDate: Instant? = null,
    val repeatPattern: String? = null,
    val initialStreak: Int = 0,
    val checklistJson: String? = null,
    val remindersJson: String? = null,
)
