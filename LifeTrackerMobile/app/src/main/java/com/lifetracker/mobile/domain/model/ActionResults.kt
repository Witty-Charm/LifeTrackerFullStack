package com.lifetracker.mobile.domain.model

import kotlin.time.Instant

data class TaskCompletionResult(
    val taskId: Int,
    val taskTitle: String,
    val xpGained: Long,
    val goldGained: Int,
    val leveledUp: Boolean,
    val newLevel: Int,
    val streakBonus: Int,
    val currentStreak: Int,
    val message: String,
    val heroSnapshot: HeroSnapshot,
)

data class TaskFailureResult(
    val taskId: Int,
    val taskTitle: String,
    val damageDealt: Int,
    val goldLost: Int,
    val heroDied: Boolean,
    val streakBroken: Boolean,
    val streakPenalty: StreakPenaltyInfo?,
    val message: String,
    val heroSnapshot: HeroSnapshot,
)

data class HeroSnapshot(
    val heroId: Int,
    val level: Int,
    val currentXp: Long,
    val xpForNextLevel: Long,
    val currentHp: Int,
    val maxHp: Int,
    val gold: Int,
    val deathCount: Int,
    val dailyCompletions: Int,
    val dailyCompletionsMax: Int,
)

data class StreakPenaltyInfo(
    val streakDays: Int,
    val xpLost: Int,
    val goldLost: Int,
    val cooldownHours: Int,
)

data class RespawnResult(
    val heroId: Int,
    val heroName: String,
    val oldHp: Int,
    val newHp: Int,
    val maxHp: Int,
    val recoveryEndsAt: Instant?,
    val recoveryMultiplier: Double,
    val recoveryDebuffActive: Boolean,
    val deathCount: Int,
    val message: String,
)

data class HealResult(
    val heroId: Int,
    val hpHealed: Int,
    val goldSpent: Int,
    val newHp: Int,
    val maxHp: Int,
    val newGold: Int,
    val message: String,
)

data class OverdueResult(
    val overdueCount: Int,
    val penalties: List<OverduePenalty>,
    val message: String,
)

data class OverduePenalty(
    val taskId: Int,
    val taskTitle: String,
    val dueDate: Instant,
    val hpLost: Int,
    val goldLost: Int,
    val heroDied: Boolean,
    val streakBroken: Boolean,
)
