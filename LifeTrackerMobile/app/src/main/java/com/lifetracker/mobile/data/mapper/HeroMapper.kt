package com.lifetracker.mobile.data.mapper

import com.lifetracker.mobile.data.remote.dto.HealResponse
import com.lifetracker.mobile.data.remote.dto.HeroDto
import com.lifetracker.mobile.data.remote.dto.HeroStatsDto
import com.lifetracker.mobile.data.remote.dto.RespawnResponse
import com.lifetracker.mobile.domain.model.HealResult
import com.lifetracker.mobile.domain.model.HeroDomain
import com.lifetracker.mobile.domain.model.HeroStatsDomain
import com.lifetracker.mobile.domain.model.RespawnResult

fun HeroDto.toDomain(): HeroDomain = HeroDomain(
    id = id,
    name = name,
    level = level,
    currentXp = xp,
    maxXp = maxXP,
    currentHp = hp,
    maxHp = maxHP,
    gold = gold,
    isDead = isDead,
    deathCount = deathCount,
    isInRecovery = isInRecovery,
    recoveryMultiplier = recoveryMultiplier,
    dailyCompletions = dailyCompletions,
    dailyCompletionsMax = dailyCompletionsMax,
)

fun HeroStatsDto.toDomain(): HeroStatsDomain = HeroStatsDomain(
    id = id,
    name = name,
    level = level,
    currentXp = currentXp,
    xpForNextLevel = xpForNextLevel,
    xpProgress = xpProgress,
    totalXpEarned = totalXpEarned,
    currentHp = currentHp,
    maxHp = maxHp,
    hpPercent = hpPercent,
    gold = gold,
    totalGoldEarned = totalGoldEarned,
    totalGoldSpent = totalGoldSpent,
    isDead = isDead,
    deathCount = deathCount,
    deathTime = deathTime,
    dailyCompletions = dailyCompletions,
    dailyCompletionsMax = dailyCompletionsMax,
    dailyProgress = dailyProgress,
    dailyResetTime = dailyResetTime,
    xpMultiplier = xpMultiplier,
    goldMultiplier = goldMultiplier,
    isInPenaltyPeriod = isInPenaltyPeriod,
    penaltyEndsAt = penaltyEndsAt,
    isInRecovery = isInRecovery,
    recoveryEndsAt = recoveryEndsAt,
    recoveryMultiplier = recoveryMultiplier,
    activeStreaks = activeStreaks,
    longestStreak = longestStreak,
    createdDate = createdDate,
    updatedAt = updatedAt,
)

fun RespawnResponse.toDomain(): RespawnResult = RespawnResult(
    heroId = heroId,
    heroName = heroName,
    oldHp = oldHp,
    newHp = newHp,
    maxHp = maxHp,
    recoveryEndsAt = recoveryEndsAt,
    recoveryMultiplier = recoveryMultiplier,
    recoveryDebuffActive = recoveryDebuffActive,
    deathCount = deathCount,
    message = message,
)

fun HealResponse.toDomain(): HealResult = HealResult(
    heroId = heroId,
    hpHealed = hpHealed,
    goldSpent = goldSpent,
    newHp = newHp,
    maxHp = maxHp,
    newGold = newGold,
    message = message,
)