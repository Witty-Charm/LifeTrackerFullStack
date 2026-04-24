package com.lifetracker.mobile.ui.components

import com.lifetracker.mobile.domain.model.HabitPolarity
import com.lifetracker.mobile.ui.model.TaskPendingAction
import com.lifetracker.mobile.ui.model.TaskUi
import com.lifetracker.mobile.ui.model.UiTaskType
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
        val task = testTask(UiTaskType.OneTime, HabitPolarity.Both).copy(pendingAction = TaskPendingAction.Complete)

        assertFalse(positiveActionClickable(task, canAct = false))
        assertFalse(negativeActionClickable(task, canAct = false))
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
