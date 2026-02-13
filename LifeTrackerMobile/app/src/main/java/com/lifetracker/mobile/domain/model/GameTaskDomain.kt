package com.lifetracker.mobile.domain.model

import com.lifetracker.mobile.data.remote.dto.TaskDifficulty
import com.lifetracker.mobile.data.remote.dto.TaskType
import kotlin.time.Instant

data class GameTaskDomain(
    val id: Int,
    val heroId: Int,
    val title: String,
    val description: String,
    val type: TaskType,
    val difficulty: TaskDifficulty,
    val isCompleted: Boolean,
    val isActive: Boolean,
    val dueDate: Instant?,
    val isOverdue: Boolean,
    val completionCount: Int,
    val failCount: Int,
    val lastCompletedAt: Instant?,
    val baseXp: Int,
    val baseGold: Int,
    val hpPenalty: Int,
    val goldPenalty: Int,
    val streak: StreakDomain?,
)

data class StreakDomain(
    val currentDays: Int,
    val bonusXpPercent: Int,
    val multiplier: Double,
    val isFrozen: Boolean,
    val isShieldActive: Boolean,
)
