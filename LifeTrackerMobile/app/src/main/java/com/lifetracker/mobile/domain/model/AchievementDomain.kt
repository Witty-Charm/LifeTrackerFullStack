package com.lifetracker.mobile.domain.model

import kotlin.time.Instant

data class AchievementDomain(
    val key: String,
    val title: String,
    val description: String,
    val category: String,
    val threshold: Int,
    val sortOrder: Int,
    val goldReward: Int,
    val unlocked: Boolean,
    val unlockedAt: Instant?,
)
