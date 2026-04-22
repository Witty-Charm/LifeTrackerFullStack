package com.lifetracker.mobile.data.mapper

import com.lifetracker.mobile.data.local.entity.HeroEntity
import com.lifetracker.mobile.data.local.entity.TaskEntity
import com.lifetracker.mobile.domain.model.GameTaskDomain
import com.lifetracker.mobile.domain.model.HabitPolarity
import com.lifetracker.mobile.domain.model.HeroDomain
import com.lifetracker.mobile.domain.model.StreakDomain
import com.lifetracker.mobile.domain.model.TaskType
import kotlin.time.Instant

fun HeroEntity.toDomain(): HeroDomain =
    HeroDomain(
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
        xpBoostPercent = xpBoostPercent,
        xpBoostTasksRemaining = xpBoostTasksRemaining,
        dailyCompletions = dailyCompletions,
        dailyCompletionsMax = dailyCompletionsMax,
    )

fun HeroDomain.toEntity(pendingSync: Boolean = false): HeroEntity =
    HeroEntity(
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
        xpBoostPercent = xpBoostPercent,
        xpBoostTasksRemaining = xpBoostTasksRemaining,
        dailyCompletions = dailyCompletions,
        dailyCompletionsMax = dailyCompletionsMax,
        pendingSync = pendingSync,
    )

fun TaskEntity.toDomain(): GameTaskDomain =
    GameTaskDomain(
        id = id,
        heroId = heroId,
        title = title,
        description = description,
        type = type,
        difficulty = difficulty,
        habitPolarity = if (type == TaskType.Habit) habitPolarity ?: HabitPolarity.Both else HabitPolarity.Both,
        isCompleted = isCompleted,
        isActive = isActive,
        dueDate = dueDate?.let { Instant.fromEpochMilliseconds(it) },
        repeatPattern = repeatPattern,
        checklistJson = checklistJson,
        remindersJson = remindersJson,
        isOverdue = isOverdue,
        completionCount = completionCount,
        failCount = failCount,
        lastCompletedAt = lastCompletedAt?.let { Instant.fromEpochMilliseconds(it) },
        overdueProcessedAt = overdueProcessedAt?.let { Instant.fromEpochMilliseconds(it) },
        baseXp = baseXp,
        baseGold = baseGold,
        hpPenalty = hpPenalty,
        goldPenalty = goldPenalty,
        streak =
            if (streakCurrentDays != null) {
                StreakDomain(
                    currentDays = streakCurrentDays,
                    bonusXpPercent = streakBonusXpPercent ?: 0,
                    multiplier = streakMultiplier ?: 1.0,
                    isFrozen = streakIsFrozen ?: false,
                    isShieldActive = streakIsShieldActive ?: false,
                )
            } else {
                null
            },
        pendingSync = pendingSync,
        syncError = syncError,
    )

fun GameTaskDomain.toEntity(pendingSync: Boolean = this.pendingSync): TaskEntity =
    TaskEntity(
        id = id,
        heroId = heroId,
        title = title,
        description = description,
        type = type,
        difficulty = difficulty,
        habitPolarity = if (type == TaskType.Habit) habitPolarity else HabitPolarity.Both,
        isCompleted = isCompleted,
        isActive = isActive,
        dueDate = dueDate?.toEpochMilliseconds(),
        repeatPattern = repeatPattern,
        checklistJson = checklistJson,
        remindersJson = remindersJson,
        isOverdue = isOverdue,
        completionCount = completionCount,
        failCount = failCount,
        lastCompletedAt = lastCompletedAt?.toEpochMilliseconds(),
        overdueProcessedAt = overdueProcessedAt?.toEpochMilliseconds(),
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
        syncError = syncError,
    )
