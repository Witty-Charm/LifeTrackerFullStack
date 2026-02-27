package com.lifetracker.mobile.ui.mapper

import com.lifetracker.mobile.domain.model.GameTaskDomain
import com.lifetracker.mobile.domain.model.HeroDomain
import com.lifetracker.mobile.domain.model.TaskDifficulty
import com.lifetracker.mobile.domain.model.TaskType
import com.lifetracker.mobile.ui.model.UiTaskType
import com.lifetracker.mobile.ui.model.HeroStatusBadge
import com.lifetracker.mobile.ui.model.HeroUi
import com.lifetracker.mobile.ui.model.TaskUi
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.time.Instant

fun HeroDomain.toUi(): HeroUi = HeroUi(
    id = id,
    name = name,
    level = level,
    xpText = "%,d / %,d XP".format(currentXp, maxXp),
    xpProgress = xpProgress,
    hpText = "$currentHp / $maxHp HP",
    hpProgress = hpProgress,
    goldText = "%,d Gold".format(gold),
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
    type = when (type) {
        TaskType.Habit   -> UiTaskType.Habit
        TaskType.OneTime -> UiTaskType.OneTime
        TaskType.Unknown -> UiTaskType.Unknown
    },
    difficultyLabel = when (difficulty) {
        TaskDifficulty.Easy    -> "Easy"
        TaskDifficulty.Medium  -> "Medium"
        TaskDifficulty.Hard    -> "Hard"
        TaskDifficulty.Epic    -> "Epic"
        TaskDifficulty.Unknown -> "Unknown"
    },
    difficultyColor = when (difficulty) {
        TaskDifficulty.Easy   -> 0xFF4CAF50
        TaskDifficulty.Medium -> 0xFFFFC107
        TaskDifficulty.Hard   -> 0xFFFF5722
        TaskDifficulty.Epic   -> 0xFF9C27B0
        TaskDifficulty.Unknown -> 0xFF9E9E9E
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
    val javaDate = this.toLocalDateTime(TimeZone.currentSystemDefault()).date.toJavaLocalDate()

    return DateTimeFormatter
        .ofLocalizedDate(FormatStyle.SHORT)
        .withLocale(Locale.getDefault())
        .format(javaDate)
}