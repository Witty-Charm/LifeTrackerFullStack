package com.lifetracker.mobile.constants

import com.lifetracker.mobile.domain.model.TaskDifficulty
import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.pow

object GameConstants {
    const val BASE_XP: Int = 100
    const val XP_EXPONENT: Double = 1.8
    const val SCALE_FACTOR: Double = 50.0

    const val BASE_HP: Int = 50
    const val HP_PER_LEVEL: Int = 5
    const val MAX_LEVEL: Int = 999

    const val DEATH_HP_RESET_PERCENT: Double = 0.25
    const val DEATH_XP_PENALTY_PERCENT: Double = 0.10
    const val DEATH_GOLD_PENALTY_PERCENT: Double = 0.20
    const val DEATH_STREAK_PENALTY_PERCENT: Double = 0.50
    const val RECOVERY_DEBUFF_HOURS: Int = 4
    const val RECOVERY_DEBUFF_MULTIPLIER: Double = 0.75

    const val STREAK_MULTIPLIER_COEFF: Double = 0.15
    const val MAX_FREEZE_CHARGES: Int = 3
    const val STREAK_TIER_DAYS: Int = 30

    const val DAILY_TASK_CAP: Int = 50

    data class Reward(val xp: Int, val gold: Int)
    data class Penalty(val hpLoss: Int, val goldLoss: Int)
    data class StreakBreakPenalty(val hpLoss: Int, val goldLoss: Int, val debuffHours: Int)

    fun getDifficultyMultiplier(difficulty: TaskDifficulty): Double = when (difficulty) {
        TaskDifficulty.Easy   -> 1.0
        TaskDifficulty.Medium -> 1.5
        TaskDifficulty.Hard   -> 2.5
        TaskDifficulty.Epic   -> 4.0
    }

    fun getHabitReward(difficulty: TaskDifficulty): Reward = when (difficulty) {
        TaskDifficulty.Easy   -> Reward(xp = 10, gold = 5)
        TaskDifficulty.Medium -> Reward(xp = 25, gold = 12)
        TaskDifficulty.Hard   -> Reward(xp = 50, gold = 25)
        TaskDifficulty.Epic   -> Reward(xp = 100, gold = 50)
    }

    fun getOneTimeReward(difficulty: TaskDifficulty): Reward = when (difficulty) {
        TaskDifficulty.Easy   -> Reward(xp = 15, gold = 8)
        TaskDifficulty.Medium -> Reward(xp = 35, gold = 18)
        TaskDifficulty.Hard   -> Reward(xp = 70, gold = 35)
        TaskDifficulty.Epic   -> Reward(xp = 150, gold = 75)
    }

    fun getHabitPenalty(difficulty: TaskDifficulty): Penalty = when (difficulty) {
        TaskDifficulty.Easy   -> Penalty(hpLoss = 5, goldLoss = 0)
        TaskDifficulty.Medium -> Penalty(hpLoss = 10, goldLoss = 5)
        TaskDifficulty.Hard   -> Penalty(hpLoss = 20, goldLoss = 15)
        TaskDifficulty.Epic   -> Penalty(hpLoss = 35, goldLoss = 30)
    }

    fun getOneTimePenalty(difficulty: TaskDifficulty): Penalty = when (difficulty) {
        TaskDifficulty.Easy   -> Penalty(hpLoss = 3, goldLoss = 0)
        TaskDifficulty.Medium -> Penalty(hpLoss = 7, goldLoss = 5)
        TaskDifficulty.Hard   -> Penalty(hpLoss = 15, goldLoss = 15)
        TaskDifficulty.Epic   -> Penalty(hpLoss = 25, goldLoss = 30)
    }

    fun getStreakBreakPenalty(streakDays: Int): StreakBreakPenalty = when {
        streakDays <= 7  -> StreakBreakPenalty(hpLoss = 0, goldLoss = 0, debuffHours = 0)
        streakDays <= 30 -> StreakBreakPenalty(hpLoss = 50, goldLoss = 25, debuffHours = 24)
        streakDays <= 90 -> StreakBreakPenalty(hpLoss = 150, goldLoss = 75, debuffHours = 48)
        else             -> StreakBreakPenalty(hpLoss = 300, goldLoss = 150, debuffHours = 72)
    }

    fun calculateXpForLevel(level: Int): Long =
        floor(BASE_XP * level.toDouble().pow(XP_EXPONENT) * (1 + level / SCALE_FACTOR)).toLong()

    fun calculateMaxHp(level: Int): Int = BASE_HP + (HP_PER_LEVEL * level)

    fun calculateStreakMultiplier(streakDays: Int): Double =
        if (streakDays <= 0) 1.0
        else 1.0 + log2((streakDays + 1).toDouble()) * STREAK_MULTIPLIER_COEFF

    fun calculateLevelScaling(heroLevel: Int): Double = 1.0 + (heroLevel / 100.0)
}