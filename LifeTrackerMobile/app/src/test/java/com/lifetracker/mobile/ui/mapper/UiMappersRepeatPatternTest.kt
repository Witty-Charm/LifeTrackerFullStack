package com.lifetracker.mobile.ui.mapper

import com.lifetracker.mobile.domain.model.GameTaskDomain
import com.lifetracker.mobile.domain.model.HabitPolarity
import com.lifetracker.mobile.domain.model.TaskDifficulty
import com.lifetracker.mobile.domain.model.TaskType
import org.junit.Assert.assertEquals
import org.junit.Test

class UiMappersRepeatPatternTest {
    @Test
    fun toUi_legacyNonDailyRepeatPattern_rendersSafeLegacyLabel() {
        val task =
            GameTaskDomain(
                id = 1,
                heroId = 1,
                title = "Legacy daily",
                description = "",
                type = TaskType.Daily,
                difficulty = TaskDifficulty.Easy,
                habitPolarity = HabitPolarity.Both,
                isCompleted = false,
                isActive = true,
                dueDate = null,
                repeatPattern = "WEEKLY:1",
                checklistJson = null,
                remindersJson = null,
                isOverdue = false,
                completionCount = 0,
                failCount = 0,
                lastCompletedAt = null,
                overdueProcessedAt = null,
                baseXp = 10,
                baseGold = 5,
                hpPenalty = 1,
                goldPenalty = 1,
                streak = null,
                pendingSync = false,
                syncError = null,
            )

        val ui = task.toUi()

        assertEquals("Legacy schedule", ui.repeatPatternText)
    }
}
