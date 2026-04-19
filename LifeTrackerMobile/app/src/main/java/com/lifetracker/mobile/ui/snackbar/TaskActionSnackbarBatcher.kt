package com.lifetracker.mobile.ui.snackbar

import com.lifetracker.mobile.ui.model.TaskActionFeedback

class TaskActionSnackbarBatcher {
    private var completionCount = 0
    private var failureCount = 0
    private var shieldAbsorbedCount = 0
    private var totalXpGained = 0L
    private var totalGoldGained = 0
    private var totalHpLost = 0
    private var totalGoldLost = 0
    private var highestLevelReached: Int? = null

    fun enqueue(feedback: TaskActionFeedback) {
        when (feedback) {
            is TaskActionFeedback.Completed -> {
                completionCount++
                totalXpGained += feedback.xpGained
                totalGoldGained += feedback.goldGained
                if (feedback.leveledUp && feedback.newLevel != null) {
                    highestLevelReached = maxOf(highestLevelReached ?: feedback.newLevel, feedback.newLevel)
                }
            }

            is TaskActionFeedback.Failed -> {
                if (feedback.shieldAbsorbed) {
                    shieldAbsorbedCount++
                } else {
                    failureCount++
                    totalHpLost += feedback.hpLost
                    totalGoldLost += feedback.goldLost
                }
            }
        }
    }

    fun flush(): String? {
        val actionCount = completionCount + failureCount + shieldAbsorbedCount
        if (actionCount == 0) return null

        val message =
            when {
                actionCount == 1 && completionCount == 1 -> {
                    buildSingleCompletionMessage()
                }

                actionCount == 1 && failureCount == 1 -> {
                    buildSingleFailureMessage()
                }

                actionCount == 1 && shieldAbsorbedCount == 1 -> {
                    "Failure was absorbed by shield"
                }

                completionCount > 0 && failureCount == 0 && shieldAbsorbedCount == 0 -> {
                    "$completionCount ${taskWord(completionCount)}: +$totalXpGained XP, +$totalGoldGained Gold${levelSuffixOrEmpty()}"
                }

                completionCount == 0 && failureCount == 0 && shieldAbsorbedCount > 0 -> {
                    shieldMessage(shieldAbsorbedCount)
                }

                else -> {
                    buildMixedMessage(actionCount)
                }
            }

        reset()
        return message
    }

    private fun buildSingleCompletionMessage(): String = "+$totalXpGained XP, +$totalGoldGained Gold${levelSuffixOrEmpty()}"

    private fun buildSingleFailureMessage(): String = "-$totalHpLost HP, -$totalGoldLost Gold"

    private fun buildMixedMessage(actionCount: Int): String {
        val parts = mutableListOf<String>()
        if (totalXpGained > 0) parts += "+$totalXpGained XP"
        if (totalGoldGained > 0) parts += "+$totalGoldGained Gold"
        if (totalHpLost > 0) parts += "-$totalHpLost HP"
        if (totalGoldLost > 0) parts += "-$totalGoldLost Gold"
        if (shieldAbsorbedCount > 0) parts += shieldMessage(shieldAbsorbedCount).replaceFirstChar { it.lowercase() }
        levelSuffix()?.let(parts::add)
        return "$actionCount ${actionWord(actionCount)}: ${parts.joinToString(", ")}"
    }

    private fun levelSuffix(): String? = highestLevelReached?.let { ", Level $it" }

    private fun levelSuffixOrEmpty(): String = levelSuffix().orEmpty()

    private fun shieldMessage(count: Int): String =
        when (count) {
            1 -> "Failure was absorbed by shield"
            else -> "$count failures were absorbed by shield"
        }

    private fun taskWord(count: Int): String = if (count == 1) "task" else "tasks"

    private fun actionWord(count: Int): String = if (count == 1) "action" else "actions"

    private fun reset() {
        completionCount = 0
        failureCount = 0
        shieldAbsorbedCount = 0
        totalXpGained = 0
        totalGoldGained = 0
        totalHpLost = 0
        totalGoldLost = 0
        highestLevelReached = null
    }
}
