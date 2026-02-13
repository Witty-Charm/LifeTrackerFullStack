package com.lifetracker.mobile.domain.mapper

import com.lifetracker.mobile.data.remote.dto.CompleteTaskResponse
import com.lifetracker.mobile.data.remote.dto.FailTaskResponse
import com.lifetracker.mobile.data.remote.dto.OverdueCheckResponse
import com.lifetracker.mobile.data.remote.dto.StreakInfoDto
import com.lifetracker.mobile.data.remote.dto.TaskDto
import com.lifetracker.mobile.domain.model.GameTaskDomain
import com.lifetracker.mobile.domain.model.HeroSnapshot
import com.lifetracker.mobile.domain.model.OverduePenalty
import com.lifetracker.mobile.domain.model.OverdueResult
import com.lifetracker.mobile.domain.model.StreakDomain
import com.lifetracker.mobile.domain.model.StreakPenaltyInfo
import com.lifetracker.mobile.domain.model.TaskCompletionResult
import com.lifetracker.mobile.domain.model.TaskFailureResult

fun TaskDto.toDomain(): GameTaskDomain = GameTaskDomain(
    id = id,
    heroId = heroId,
    title = title,
    description = description,
    type = type,
    difficulty = difficulty,
    isCompleted = isCompleted,
    isActive = isActive,
    dueDate = dueDate,
    isOverdue = isOverdue,
    completionCount = completionCount,
    failCount = failCount,
    lastCompletedAt = lastCompletedAt,
    baseXp = baseXp,
    baseGold = baseGold,
    hpPenalty = hpPenalty,
    goldPenalty = goldPenalty,
    streak = streakInfo?.toDomain(),
)

fun StreakInfoDto.toDomain(): StreakDomain = StreakDomain(
    currentDays = currentDays,
    bonusXpPercent = bonusXpPercent,
    multiplier = multiplier,
    isFrozen = isFrozen,
    isShieldActive = isShieldActive,
)

fun CompleteTaskResponse.toDomain(): TaskCompletionResult = TaskCompletionResult(
    taskId = taskId,
    taskTitle = taskTitle,
    xpGained = xpGained,
    goldGained = goldGained,
    leveledUp = leveledUp,
    newLevel = newLevel,
    streakBonus = streakBonus,
    currentStreak = currentStreak,
    message = message,
    heroSnapshot = HeroSnapshot(
        heroId = heroId,
        level = newLevel,
        currentXp = newXp,
        xpForNextLevel = xpForNextLevel,
        currentHp = newHp,
        maxHp = maxHp,
        gold = newGold,
        deathCount = 0,
        dailyCompletions = dailyCompletions,
        dailyCompletionsMax = maxDailyCompletions,
    ),
)

fun FailTaskResponse.toDomain(): TaskFailureResult = TaskFailureResult(
    taskId = taskId,
    taskTitle = taskTitle,
    damageDealt = damageDealt,
    goldLost = goldLost,
    heroDied = heroDied,
    streakBroken = streakBroken,
    streakPenalty = streakPenalty?.let {
        StreakPenaltyInfo(
            streakDays = it.streakDays,
            xpLost = it.xpLost,
            goldLost = it.goldLost,
            cooldownHours = it.cooldownHours,
        )
    },
    message = message,
    heroSnapshot = HeroSnapshot(
        heroId = heroId,
        level = currentLevel,
        currentXp = currentXp,
        xpForNextLevel = 0,
        currentHp = newHp,
        maxHp = maxHp,
        gold = newGold,
        deathCount = deathCount,
        dailyCompletions = 0,
        dailyCompletionsMax = 0,
    ),
)

fun OverdueCheckResponse.toDomain(): OverdueResult = OverdueResult(
    overdueCount = overdueCount,
    penalties = penalties?.map {
        OverduePenalty(
            taskId = it.taskId,
            taskTitle = it.taskTitle,
            dueDate = it.dueDate,
            hpLost = it.hpLost,
            goldLost = it.goldLost,
            heroDied = it.heroDied,
            streakBroken = it.streakBroken,
        )
    } ?: emptyList(),
    message = message,
)