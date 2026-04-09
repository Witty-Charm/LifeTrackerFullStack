package com.lifetracker.mobile.data.remote.dto

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class HeroDto(
    val id: Int,
    val name: String,
    val level: Int,
    val xp: Long,
    val maxXP: Long,
    val hp: Int,
    val maxHP: Int,
    val gold: Int,
    val isDead: Boolean,
    val deathCount: Int,
    val isInRecovery: Boolean,
    val recoveryMultiplier: Double,
    val xpBoostPercent: Int = 0,
    val xpBoostTasksRemaining: Int = 0,
    val dailyCompletions: Int,
    val dailyCompletionsMax: Int,
)

@Serializable
data class CreateHeroRequest(
    val name: String,
    val startingGold: Int? = null,
)

@Serializable
data class HeroStatsDto(
    val id: Int,
    val name: String,

    val level: Int,
    val currentXp: Long,
    val xpForNextLevel: Long,
    val xpProgress: Double,
    val totalXpEarned: Long,

    val currentHp: Int,
    val maxHp: Int,
    val hpPercent: Double,

    val gold: Int,
    val totalGoldEarned: Long,
    val totalGoldSpent: Long,

    val isDead: Boolean,
    val deathCount: Int,
    val deathTime: Instant? = null,

    val dailyCompletions: Int,
    val dailyCompletionsMax: Int,
    val dailyProgress: Double,
    val dailyResetTime: Instant,

    val xpMultiplier: Double,
    val goldMultiplier: Double,

    val isInPenaltyPeriod: Boolean,
    val penaltyEndsAt: Instant? = null,
    val isInRecovery: Boolean,
    val recoveryEndsAt: Instant? = null,
    val recoveryMultiplier: Double,

    val activeStreaks: Int,
    val longestStreak: Int,

    val createdDate: Instant,
    val updatedAt: Instant,
)

@Serializable
data class RespawnResponse(
    val success: Boolean,
    val heroId: Int,
    val heroName: String,
    val oldHp: Int,
    val newHp: Int,
    val maxHp: Int,
    val recoveryDebuffActive: Boolean,
    val recoveryEndsAt: Instant? = null,
    val recoveryMultiplier: Double,
    val deathCount: Int,
    val message: String,
)

@Serializable
data class HealResponse(
    val success: Boolean,
    val heroId: Int,
    val hpHealed: Int,
    val goldSpent: Int,
    val newHp: Int,
    val maxHp: Int,
    val newGold: Int,
    val message: String,
)

