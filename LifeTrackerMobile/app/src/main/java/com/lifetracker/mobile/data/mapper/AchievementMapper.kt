package com.lifetracker.mobile.data.mapper

import com.lifetracker.mobile.data.remote.dto.AchievementDto
import com.lifetracker.mobile.data.remote.dto.HeroAchievementsResponseDto
import com.lifetracker.mobile.domain.model.AchievementDomain

fun AchievementDto.toDomain(): AchievementDomain =
    AchievementDomain(
        key = key,
        title = title,
        description = description,
        category = category,
        threshold = threshold,
        sortOrder = sortOrder,
        goldReward = goldReward,
        unlocked = unlocked,
        unlockedAt = unlockedAt,
    )

fun HeroAchievementsResponseDto.toDomain(): List<AchievementDomain> = achievements.map { it.toDomain() }
