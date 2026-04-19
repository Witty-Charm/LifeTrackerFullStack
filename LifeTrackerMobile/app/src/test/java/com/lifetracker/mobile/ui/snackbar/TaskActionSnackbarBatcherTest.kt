package com.lifetracker.mobile.ui.snackbar

import com.lifetracker.mobile.ui.model.TaskActionFeedback
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TaskActionSnackbarBatcherTest {
    private val batcher = TaskActionSnackbarBatcher()

    @Test
    fun flush_returnsNull_whenNoEventsWereQueued() {
        assertNull(batcher.flush())
    }

    @Test
    fun flush_returnsSingleCompletionSummary_whenOneCompletionWasQueued() {
        batcher.enqueue(
            TaskActionFeedback.Completed(
                xpGained = 25,
                goldGained = 10,
                leveledUp = false,
                newLevel = null,
            ),
        )

        assertEquals("+25 XP, +10 Gold", batcher.flush())
    }

    @Test
    fun flush_sumsMultipleCompletionsIntoSingleSummary() {
        batcher.enqueue(
            TaskActionFeedback.Completed(
                xpGained = 25,
                goldGained = 10,
                leveledUp = false,
                newLevel = null,
            ),
        )
        batcher.enqueue(
            TaskActionFeedback.Completed(
                xpGained = 15,
                goldGained = 5,
                leveledUp = false,
                newLevel = null,
            ),
        )

        assertEquals("2 tasks: +40 XP, +15 Gold", batcher.flush())
    }

    @Test
    fun flush_sumsMixedCompletionAndFailureIntoSingleSummary() {
        batcher.enqueue(
            TaskActionFeedback.Completed(
                xpGained = 50,
                goldGained = 20,
                leveledUp = false,
                newLevel = null,
            ),
        )
        batcher.enqueue(
            TaskActionFeedback.Failed(
                hpLost = 7,
                goldLost = 3,
                shieldAbsorbed = false,
            ),
        )

        assertEquals("2 actions: +50 XP, +20 Gold, -7 HP, -3 Gold", batcher.flush())
    }

    @Test
    fun flush_reportsShieldAbsorbedSeparatelyWithoutHpOrGoldLoss() {
        batcher.enqueue(
            TaskActionFeedback.Failed(
                hpLost = 0,
                goldLost = 0,
                shieldAbsorbed = true,
            ),
        )
        batcher.enqueue(
            TaskActionFeedback.Failed(
                hpLost = 0,
                goldLost = 0,
                shieldAbsorbed = true,
            ),
        )

        assertEquals("2 failures were absorbed by shield", batcher.flush())
    }
}
