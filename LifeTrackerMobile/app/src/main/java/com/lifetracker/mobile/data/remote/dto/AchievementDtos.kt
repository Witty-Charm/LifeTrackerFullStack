package com.lifetracker.mobile.data.remote.dto

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class AchievementDto(
    val key: String,
    val title: String,
    val description: String,
    val category: String,
    val threshold: Int,
    val sortOrder: Int,
    val goldReward: Int,
    val unlocked: Boolean,
    val unlockedAt: Instant? = null,
)

@Serializable
data class HeroAchievementsResponseDto(
    val heroId: Int,
    val totalCount: Int,
    val unlockedCount: Int,
    val achievements: List<AchievementDto>,
)
