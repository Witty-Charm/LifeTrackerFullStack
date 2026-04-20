package com.lifetracker.mobile.ui.mapper

import com.lifetracker.mobile.domain.model.AchievementDomain
import com.lifetracker.mobile.ui.model.AchievementUi
import kotlinx.collections.immutable.toPersistentList

fun AchievementDomain.toUi(): AchievementUi =
    AchievementUi(
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

fun List<AchievementDomain>.toUi() = map { it.toUi() }.toPersistentList()
