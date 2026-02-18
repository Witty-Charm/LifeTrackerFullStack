package com.lifetracker.mobile.ui.mapper

import android.content.Context
import com.lifetracker.mobile.R
import com.lifetracker.mobile.data.remote.dto.TaskDifficulty
import com.lifetracker.mobile.data.remote.dto.TaskType
import com.lifetracker.mobile.domain.model.GameTaskDomain
import com.lifetracker.mobile.domain.model.HeroDomain
import com.lifetracker.mobile.ui.model.HeroStatusBadge
import com.lifetracker.mobile.ui.model.HeroUi
import com.lifetracker.mobile.ui.model.TaskUi
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import java.text.NumberFormat
import kotlin.time.Instant

private val numFmt: NumberFormat = NumberFormat.getIntegerInstance()

fun HeroDomain.toUi(): HeroUi = HeroUi(
    id = id,
    name = name,
    level = level,
    xpText = "${numFmt.format(currentXp)} / ${numFmt.format(maxXp)} XP",
    xpProgress = xpProgress,
    hpText = "$currentHp / $maxHp HP",
    hpProgress = hpProgress,
    goldText = "${numFmt.format(gold)} Gold",
    isDead = isDead,
    isInRecovery = isInRecovery,
    dailyText = "$dailyCompletions / $dailyCompletionsMax tasks today",
    dailyProgress = dailyProgress,
    statusBadge = when {
        isDead -> HeroStatusBadge.Dead
        isInRecovery -> HeroStatusBadge.Recovery
        else -> HeroStatusBadge.Alive
    },
)

fun GameTaskDomain.toUi(): TaskUi = TaskUi(
    id = id,
    title = title,
    description = description,
    type = type,
    difficulty = difficulty,
    difficultyColor = when (difficulty) {
        TaskDifficulty.Easy   -> 0xFF4CAF50
        TaskDifficulty.Medium -> 0xFFFFC107
        TaskDifficulty.Hard   -> 0xFFFF5722
        TaskDifficulty.Epic   -> 0xFF9C27B0
    },
    isCompleted = isCompleted,
    isOverdue = isOverdue,
    dueDateText = dueDate?.toDisplayDate(),
    rewardText = "+$baseXp XP +$baseGold Gold",
    penaltyText = "-$hpPenalty HP -$goldPenalty Gold",
    streakText = streak?.takeIf { it.currentDays > 0 }
        ?.let { "\uD83D\uDD25 ${it.currentDays} days (+${it.bonusXpPercent}%)" },
)

private fun Instant.toDisplayDate(): String {
    val ld = toLocalDateTime(TimeZone.currentSystemDefault())
    return "%02d/%02d/%d".format(ld.month.number, ld.day, ld.year)
}