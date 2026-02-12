package com.lifetracker.mobile.domain.model

import kotlin.time.Instant

data class HeroStatsDomain(
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
    val deathTime: Instant?,

    val dailyCompletions: Int,
    val dailyCompletionsMax: Int,
    val dailyProgress: Double,
    val dailyResetTime: Instant,

    val xpMultiplier: Double,
    val goldMultiplier: Double,

    val isInPenaltyPeriod: Boolean,
    val penaltyEndsAt: Instant?,
    val isInRecovery: Boolean,
    val recoveryEndsAt: Instant?,
    val recoveryMultiplier: Double,

    val activeStreaks: Int,
    val longestStreak: Int,

    val createdDate: Instant,
    val updatedAt: Instant,
)
