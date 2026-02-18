package com.lifetracker.mobile.data.remote.dto

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class TaskDto(
    val id: Int,
    val heroId: Int,
    val title: String,
    val description: String,
    val type: TaskType,
    val difficulty: TaskDifficulty,
    val isCompleted: Boolean,
    val isActive: Boolean,
    val dueDate: Instant? = null,
    val isOverdue: Boolean,
    val completionCount: Int,
    val failCount: Int,
    val lastCompletedAt: Instant? = null,
    val baseXp: Int,
    val baseGold: Int,
    val hpPenalty: Int,
    val goldPenalty: Int,
    val streakInfo: StreakInfoDto? = null,
)

@Serializable
data class StreakInfoDto(
    val currentDays: Int,
    val bonusXpPercent: Int,
    val multiplier: Double,
    val isFrozen: Boolean,
    val isShieldActive: Boolean,
)

@Serializable
data class CreateTaskRequest(
    val heroId: Int? = null,
    val title: String,
    val description: String? = null,
    val type: TaskType = TaskType.OneTime,
    val difficulty: TaskDifficulty = TaskDifficulty.Easy,
    val dueDate: Instant? = null,
    val repeatPattern: String? = null,
)

@Serializable
data class CompleteTaskResponse(
    val success: Boolean,
    val taskId: Int,
    val taskTitle: String,
    val xpGained: Long,
    val goldGained: Int,
    val heroId: Int,
    val newLevel: Int,
    val leveledUp: Boolean,
    val newXp: Long,
    val xpForNextLevel: Long,
    val xpProgress: Double,
    val newGold: Int,
    val newHp: Int,
    val maxHp: Int,
    val streakBonus: Int,
    val currentStreak: Int,
    val streakMultiplier: Double,
    val dailyCompletions: Int,
    val maxDailyCompletions: Int,
    val deathCount: Int,
    val message: String,
)

@Serializable
data class FailTaskResponse(
    val success: Boolean,
    val taskId: Int,
    val taskTitle: String,
    val damageDealt: Int,
    val goldLost: Int,
    val heroId: Int,
    val newHp: Int,
    val maxHp: Int,
    val newGold: Int,
    val currentLevel: Int,
    val currentXp: Long,
    val heroDied: Boolean,
    val deathCount: Int,
    val xpForNextLevel: Long,
    val dailyCompletions: Int,
    val maxDailyCompletions: Int,
    val streakBroken: Boolean,
    val streakPenalty: StreakPenaltyDto? = null,
    val message: String,
)

@Serializable
data class StreakPenaltyDto(
    val streakDays: Int,
    val xpLost: Int,
    val goldLost: Int,
    val cooldownHours: Int,
)

@Serializable
data class OverdueCheckResponse(
    val overdueCount: Int,
    val penalties: List<OverdueTaskPenalty>? = null,
    val message: String,
)

@Serializable
data class OverdueTaskPenalty(
    val taskId: Int,
    val taskTitle: String,
    val dueDate: Instant,
    val hpLost: Int,
    val goldLost: Int,
    val heroDied: Boolean,
    val streakBroken: Boolean,
)