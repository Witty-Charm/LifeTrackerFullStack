package com.lifetracker.mobile.data.mapper

import com.lifetracker.mobile.core.serialization.JsonDefaults
import com.lifetracker.mobile.data.local.EnumConverters
import com.lifetracker.mobile.data.local.entity.TaskEntity
import com.lifetracker.mobile.data.remote.dto.CreateTaskRequest
import com.lifetracker.mobile.data.remote.dto.TaskDto
import com.lifetracker.mobile.domain.model.HabitPolarity
import com.lifetracker.mobile.domain.model.TaskDifficulty
import com.lifetracker.mobile.domain.model.TaskType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitPolarityMapperTest {
    private val converters = EnumConverters()

    @Test
    fun taskDto_toDomain_mapsHabitPolarity() {
        val dto =
            TaskDto(
                id = 1,
                heroId = 42,
                title = "Drink water",
                description = "",
                type = com.lifetracker.mobile.data.remote.dto.TaskType.Habit,
                difficulty = com.lifetracker.mobile.data.remote.dto.TaskDifficulty.Easy,
                habitPolarity = com.lifetracker.mobile.data.remote.dto.HabitPolarity.Negative,
                isCompleted = false,
                isActive = true,
                dueDate = null,
                repeatPattern = null,
                checklistJson = null,
                remindersJson = null,
                isOverdue = false,
                completionCount = 0,
                failCount = 0,
                lastCompletedAt = null,
                overdueProcessedAt = null,
                baseXp = 10,
                baseGold = 5,
                hpPenalty = 0,
                goldPenalty = 0,
                streakInfo = null,
            )

        val domain = dto.toDomain()

        assertEquals(HabitPolarity.Negative, domain.habitPolarity)
    }

    @Test
    fun taskEntity_toDomain_fallsBackToBoth_forLegacyHabitWithoutPolarity() {
        val entity =
            TaskEntity(
                id = 2,
                heroId = 42,
                title = "No sugar",
                description = "",
                type = TaskType.Habit,
                difficulty = TaskDifficulty.Medium,
                habitPolarity = null,
                isCompleted = false,
                isActive = true,
                dueDate = null,
                repeatPattern = null,
                checklistJson = null,
                remindersJson = null,
                isOverdue = false,
                completionCount = 0,
                failCount = 0,
                lastCompletedAt = null,
                overdueProcessedAt = null,
                baseXp = 8,
                baseGold = 3,
                hpPenalty = 2,
                goldPenalty = 1,
                streakCurrentDays = null,
                streakBonusXpPercent = null,
                streakMultiplier = null,
                streakIsFrozen = null,
                streakIsShieldActive = null,
                pendingSync = false,
                syncError = null,
            )

        val domain = entity.toDomain()

        assertEquals(HabitPolarity.Both, domain.habitPolarity)
    }

    @Test
    fun enumConverter_fromHabitPolarity_fallsBackToBoth_forUnknownValue() {
        assertEquals(HabitPolarity.Both, converters.fromHabitPolarity("legacy"))
    }

    @Test
    fun createTaskRequest_serializesPolarity_withBackendFieldName() {
        val payload =
            JsonDefaults.encodeToString(
                CreateTaskRequest.serializer(),
                CreateTaskRequest(
                    heroId = 1,
                    title = "No sugar",
                    type = com.lifetracker.mobile.data.remote.dto.TaskType.Habit,
                    difficulty = com.lifetracker.mobile.data.remote.dto.TaskDifficulty.Easy,
                    habitPolarity = com.lifetracker.mobile.data.remote.dto.HabitPolarity.Negative,
                ),
            )

        assertTrue(payload.contains("\"polarity\":\"Negative\""))
    }

    @Test
    fun taskDto_deserializesPolarity_fromBackendFieldName() {
        val payload =
            """
            {
              "id": 1,
              "heroId": 42,
              "title": "No sugar",
              "description": "",
              "type": "Habit",
              "difficulty": "Easy",
              "polarity": "Negative",
              "isCompleted": false,
              "isActive": true,
              "isOverdue": false,
              "completionCount": 0,
              "failCount": 0,
              "baseXp": 10,
              "baseGold": 5,
              "hpPenalty": 2,
              "goldPenalty": 1
            }
            """.trimIndent()

        val dto = JsonDefaults.decodeFromString(TaskDto.serializer(), payload)

        assertEquals(com.lifetracker.mobile.data.remote.dto.HabitPolarity.Negative, dto.habitPolarity)
    }
}
