package com.lifetracker.mobile.domain.model

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
    val repeatPattern: String? = null,
    val checklistJson: String? = null,
    val remindersJson: String? = null,
    val isOverdue: Boolean,
    val completionCount: Int,
    val failCount: Int,
    val lastCompletedAt: Instant?,
    val overdueProcessedAt: Instant?,
    val baseXp: Int,
    val baseGold: Int,
    val hpPenalty: Int,
    val goldPenalty: Int,
    val streak: StreakDomain?,
    val pendingSync: Boolean = false,
    val syncError: String? = null,
)

data class StreakDomain(
    val currentDays: Int,
    val bonusXpPercent: Int,
    val multiplier: Double,
    val isFrozen: Boolean,
    val isShieldActive: Boolean,
)
