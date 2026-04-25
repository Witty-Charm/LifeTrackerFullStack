package com.lifetracker.mobile.data.mapper

import com.lifetracker.mobile.data.remote.dto.DailyTaskStateResponse
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskMapperDailyStateTest {
    @Test
    fun dailyTaskStateResponse_mapsDeltasToDomainCompletionResult() {
        val response =
            DailyTaskStateResponse(
                success = true,
                taskId = 7,
                taskTitle = "Daily",
                isChecked = false,
                xpDelta = -10,
                goldDelta = -5,
                heroId = 1,
                newLevel = 3,
                leveledUp = false,
                newXp = 40,
                xpForNextLevel = 100,
                xpProgress = 0.4,
                newGold = 45,
                newHp = 90,
                maxHp = 100,
                streakBonus = 0,
                currentStreak = 0,
                streakMultiplier = 1.0,
                dailyCompletions = 0,
                maxDailyCompletions = 5,
                deathCount = 0,
                xpBoostPercent = 0,
                xpBoostTasksRemaining = 0,
                unlockedAchievements = emptyList(),
                message = "Daily unchecked",
            )

        val result = response.toDomain()

        assertEquals(-10, result.xpGained)
        assertEquals(-5, result.goldGained)
        assertEquals(45, result.heroSnapshot.gold)
        assertEquals(0, result.heroSnapshot.dailyCompletions)
    }
}
