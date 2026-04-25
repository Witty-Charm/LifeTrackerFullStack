package com.lifetracker.mobile.domain.model

data class HeroDomain(
    val id: Int,
    val name: String,
    val timeZoneId: String = "UTC",
    val level: Int,
    val currentXp: Long,
    val maxXp: Long,
    val currentHp: Int,
    val maxHp: Int,
    val gold: Int,
    val isDead: Boolean,
    val deathCount: Int,
    val isInRecovery: Boolean,
    val recoveryMultiplier: Double,
    val xpBoostPercent: Int = 0,
    val xpBoostTasksRemaining: Int = 0,
    val dailyCompletions: Int,
    val dailyCompletionsMax: Int,
) {
    val xpProgress: Float
        get() = if (maxXp > 0) (currentXp.toFloat() / maxXp) else 0f

    val hpProgress: Float
        get() = if (maxHp > 0) (currentHp.toFloat() / maxHp) else 0f

    val dailyProgress: Float
        get() = if (dailyCompletionsMax > 0)
                (dailyCompletions.toFloat() / dailyCompletionsMax.toFloat()) else 0f

    val dailyRemaining: Int
        get() = (dailyCompletionsMax - dailyCompletions).coerceAtLeast(0)

    val canCompleteTasks: Boolean
        get() = !isDead && dailyCompletions < dailyCompletionsMax
}


