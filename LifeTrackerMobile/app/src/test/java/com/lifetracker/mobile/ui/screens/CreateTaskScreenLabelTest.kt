package com.lifetracker.mobile.ui.screens

import com.lifetracker.mobile.domain.model.HabitPolarity
import com.lifetracker.mobile.ui.model.UiTaskType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateTaskScreenLabelTest {
    @Test
    fun taskNameLabel_returnsHabitName_whenLockedToHabit() {
        assertEquals("Habit name", taskNameLabel(UiTaskType.Habit, lockTypeSelection = true))
    }

    @Test
    fun taskNameLabel_returnsTodoName_whenLockedToOneTime() {
        assertEquals("To do name", taskNameLabel(UiTaskType.OneTime, lockTypeSelection = true))
    }

    @Test
    fun taskNameLabel_returnsTaskName_whenTypeSelectionIsUnlocked() {
        assertEquals("Task name", taskNameLabel(UiTaskType.Habit, lockTypeSelection = false))
    }

    @Test
    fun defaultHabitPolarity_returnsBoth_forNewHabit() {
        assertEquals(HabitPolarity.Both, defaultHabitPolarity(UiTaskType.Habit))
    }

    @Test
    fun shouldShowHabitPolarity_returnsTrue_onlyForHabit() {
        assertTrue(shouldShowHabitPolarity(UiTaskType.Habit))
        assertFalse(shouldShowHabitPolarity(UiTaskType.OneTime))
    }
}
