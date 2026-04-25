package com.lifetracker.mobile.ui.components

import androidx.compose.ui.graphics.Color
import com.lifetracker.mobile.domain.model.HabitPolarity
import com.lifetracker.mobile.ui.model.TaskPendingAction
import com.lifetracker.mobile.ui.model.TaskUi
import com.lifetracker.mobile.ui.model.UiTaskType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskItemActionVisualStateTest {
    @Test
    fun positiveHabit_keepsPositiveHighlighted_whenClicksAreBlocked() {
        val task = testTask(UiTaskType.Habit, HabitPolarity.Positive)

        assertTrue(positiveActionHighlighted(task, canAct = false))
        assertFalse(negativeActionHighlighted(task, canAct = false))
        assertFalse(positiveActionClickable(task, canAct = false))
        assertFalse(negativeActionClickable(task, canAct = false))
    }

    @Test
    fun negativeHabit_keepsNegativeHighlighted_whenClicksAreBlocked() {
        val task = testTask(UiTaskType.Habit, HabitPolarity.Negative)

        assertFalse(positiveActionHighlighted(task, canAct = false))
        assertTrue(negativeActionHighlighted(task, canAct = false))
        assertFalse(positiveActionClickable(task, canAct = false))
        assertFalse(negativeActionClickable(task, canAct = false))
    }

    @Test
    fun bothHabit_keepsBothHighlighted_whenClicksAreBlocked() {
        val task = testTask(UiTaskType.Habit, HabitPolarity.Both)

        assertTrue(positiveActionHighlighted(task, canAct = false))
        assertTrue(negativeActionHighlighted(task, canAct = false))
        assertFalse(positiveActionClickable(task, canAct = false))
        assertFalse(negativeActionClickable(task, canAct = false))
    }

    @Test
    fun pendingAction_blocksActionButtons() {
        val normalTask = testTask(UiTaskType.OneTime, HabitPolarity.Both)
        val pendingTask = normalTask.copy(pendingAction = TaskPendingAction.Complete)

        assertTrue(positiveActionClickable(normalTask, canAct = true))
        assertTrue(negativeActionClickable(normalTask, canAct = true))
        assertFalse(positiveActionClickable(pendingTask, canAct = false))
        assertFalse(negativeActionClickable(pendingTask, canAct = false))
    }

    @Test
    fun daily_showsOnlyPositiveAction() {
        val task = testTask(UiTaskType.Daily, HabitPolarity.Both)

        assertTrue(task.showsPositiveAction)
        assertFalse(task.showsNegativeAction)
        assertTrue(positiveActionClickable(task, canAct = true))
        assertFalse(negativeActionClickable(task, canAct = true))
    }

    @Test
    fun checkedDaily_keepsPositiveActionClickable() {
        val task = testTask(UiTaskType.Daily, HabitPolarity.Both).copy(isCheckedToday = true)

        assertTrue(task.showsPositiveAction)
        assertFalse(task.showsNegativeAction)
        assertTrue(positiveActionClickable(task, canAct = true))
    }

    @Test
    fun checkedDaily_usesSofterCardAlpha() {
        val task = testTask(UiTaskType.Daily, HabitPolarity.Both).copy(isCheckedToday = true)

        assertEquals(0.72f, taskCardAlpha(task), 0.001f)
    }

    @Test
    fun dailyCheckmarkHelpers_matchHiddenAndVisibleStates() {
        assertEquals(0f, dailyCheckmarkAlpha(isCheckedToday = false), 0.001f)
        assertEquals(1f, dailyCheckmarkAlpha(isCheckedToday = true), 0.001f)
        assertEquals(0.82f, dailyCheckmarkScale(isCheckedToday = false), 0.001f)
        assertEquals(1f, dailyCheckmarkScale(isCheckedToday = true), 0.001f)
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
