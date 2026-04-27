package com.lifetracker.mobile.ui.model

import com.lifetracker.mobile.domain.model.HabitPolarity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskUiPolarityTest {
    @Test
    fun positiveHabit_showsBothActions_butDisablesNegative() {
        val task = testTask(type = UiTaskType.Habit, habitPolarity = HabitPolarity.Positive)

        assertTrue(task.showsPositiveAction)
        assertTrue(task.showsNegativeAction)
        assertTrue(task.positiveActionEnabled)
        assertFalse(task.negativeActionEnabled)
    }

    @Test
    fun negativeHabit_showsBothActions_butDisablesPositive() {
        val task = testTask(type = UiTaskType.Habit, habitPolarity = HabitPolarity.Negative)

        assertTrue(task.showsPositiveAction)
        assertTrue(task.showsNegativeAction)
        assertFalse(task.positiveActionEnabled)
        assertTrue(task.negativeActionEnabled)
    }

    @Test
    fun bothHabit_showsAndEnablesBothActions() {
        val task = testTask(type = UiTaskType.Habit, habitPolarity = HabitPolarity.Both)

        assertTrue(task.showsPositiveAction)
        assertTrue(task.showsNegativeAction)
        assertTrue(task.positiveActionEnabled)
        assertTrue(task.negativeActionEnabled)
    }

    @Test
    fun dailyChecked_keepsPositiveActionEnabled() {
        val task = testTask(type = UiTaskType.Daily, habitPolarity = HabitPolarity.Both).copy(isCheckedToday = true)

        assertTrue(task.showsPositiveAction)
        assertFalse(task.showsNegativeAction)
        assertTrue(task.positiveActionEnabled)
        assertFalse(task.negativeActionEnabled)
    }

    @Test
    fun dailyNotScheduledToday_disablesPositiveAction() {
        val task = testTask(type = UiTaskType.Daily, habitPolarity = HabitPolarity.Both)
            .copy(isScheduledToday = false)

        assertTrue(task.showsPositiveAction)
        assertFalse(task.positiveActionEnabled)
    }

    @Test
    fun dailyScheduledToday_keepsPositiveActionEnabled() {
        val task = testTask(type = UiTaskType.Daily, habitPolarity = HabitPolarity.Both)
            .copy(isScheduledToday = true)

        assertTrue(task.positiveActionEnabled)
    }

    @Test
    fun nonHabit_keepsBothActionsEnabled() {
        val task = testTask(type = UiTaskType.OneTime, habitPolarity = HabitPolarity.Negative)

        assertTrue(task.showsPositiveAction)
        assertTrue(task.showsNegativeAction)
        assertTrue(task.positiveActionEnabled)
        assertTrue(task.negativeActionEnabled)
    }

    private fun testTask(
        type: UiTaskType,
        habitPolarity: HabitPolarity,
    ) = TaskUi(
        id = 1,
        title = "Task",
        description = "",
        type = type,
        habitPolarity = habitPolarity,
        difficultyLabel = "Easy",
        difficultyColor = 0xFF4CAF50,
        isCompleted = false,
        isCheckedToday = false,
        isOverdue = false,
        dueDateText = null,
        rewardText = "+10 XP +5 Gold",
        penaltyText = "-0 HP -0 Gold",
        streakText = null,
        isPendingSync = false,
        pendingAction = null,
        actionError = null,
    )
}
