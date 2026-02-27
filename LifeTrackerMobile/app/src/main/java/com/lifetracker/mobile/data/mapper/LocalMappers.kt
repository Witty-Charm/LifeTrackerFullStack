package com.lifetracker.mobile.data.mapper

import com.lifetracker.mobile.data.local.entity.HeroEntity
import com.lifetracker.mobile.data.local.entity.TaskEntity
import com.lifetracker.mobile.domain.model.GameTaskDomain
import com.lifetracker.mobile.domain.model.HeroDomain
import com.lifetracker.mobile.domain.model.StreakDomain
import com.lifetracker.mobile.domain.model.TaskDifficulty
import com.lifetracker.mobile.domain.model.TaskType
import kotlin.time.Instant

fun HeroEntity.toDomain(): HeroDomain = HeroDomain(
    id = id,
    name = name,
    level = level,
    currentXp = currentXp,
    maxXp = maxXp,
    currentHp = currentHp,
    maxHp = maxHp,
    gold = gold,
    isDead = isDead,
    deathCount = deathCount,
    isInRecovery = isInRecovery,
    recoveryMultiplier = recoveryMultiplier,
    dailyCompletions = dailyCompletions,
    dailyCompletionsMax = dailyCompletionsMax,
)

fun HeroDomain.toEntity(pendingSync: Boolean = false): HeroEntity = HeroEntity(
    id = id,
    name = name,
    level = level,
    currentXp = currentXp,
    maxXp = maxXp,
    currentHp = currentHp,
    maxHp = maxHp,
    gold = gold,
    isDead = isDead,
    deathCount = deathCount,
    isInRecovery = isInRecovery,
    recoveryMultiplier = recoveryMultiplier,
    dailyCompletions = dailyCompletions,
    dailyCompletionsMax = dailyCompletionsMax,
    pendingSync = pendingSync,
)

fun TaskEntity.toDomain(): GameTaskDomain = GameTaskDomain(
    id = id,
    heroId = heroId,
    title = title,
    description = description,
    type = when (type) {
        "Habit"   -> TaskType.Habit
        "OneTime" -> TaskType.OneTime
        else      -> TaskType.Unknown
    },
    difficulty = when (difficulty) {
        "Easy"   -> TaskDifficulty.Easy
        "Medium" -> TaskDifficulty.Medium
        "Hard"   -> TaskDifficulty.Hard
        "Epic"   -> TaskDifficulty.Epic
        else     -> TaskDifficulty.Unknown
    },
    isCompleted = isCompleted,
    isActive = isActive,
    dueDate = dueDate?.let { Instant.fromEpochMilliseconds(it) },
    isOverdue = isOverdue,
    completionCount = completionCount,
    failCount = failCount,
    lastCompletedAt = lastCompletedAt?.let { Instant.fromEpochMilliseconds(it) },
    baseXp = baseXp,
    baseGold = baseGold,
    hpPenalty = hpPenalty,
    goldPenalty = goldPenalty,
    streak = if (streakCurrentDays != null) {
        StreakDomain(
            currentDays = streakCurrentDays,
            bonusXpPercent = streakBonusXpPercent ?: 0,
            multiplier = streakMultiplier ?: 1.0,
            isFrozen = streakIsFrozen ?: false,
            isShieldActive = streakIsShieldActive ?: false,
        )
    } else null,
)

fun GameTaskDomain.toEntity(pendingSync: Boolean = false): TaskEntity = TaskEntity(
    id = id,
    heroId = heroId,
    title = title,
    description = description,
    type = type.name,
    difficulty = difficulty.name,
    isCompleted = isCompleted,
    isActive = isActive,
    dueDate = dueDate?.toEpochMilliseconds(),
    isOverdue = isOverdue,
    completionCount = completionCount,
    failCount = failCount,
    lastCompletedAt = lastCompletedAt?.toEpochMilliseconds(),
    baseXp = baseXp,
    baseGold = baseGold,
    hpPenalty = hpPenalty,
    goldPenalty = goldPenalty,
    streakCurrentDays = streak?.currentDays,
    streakBonusXpPercent = streak?.bonusXpPercent,
    streakMultiplier = streak?.multiplier,
    streakIsFrozen = streak?.isFrozen,
    streakIsShieldActive = streak?.isShieldActive,
    pendingSync = pendingSync,
)
