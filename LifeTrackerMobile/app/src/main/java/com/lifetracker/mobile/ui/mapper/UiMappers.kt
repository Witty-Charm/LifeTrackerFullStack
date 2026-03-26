package com.lifetracker.mobile.ui.mapper

import com.lifetracker.mobile.domain.model.GameTaskDomain
import com.lifetracker.mobile.domain.model.HeroDomain
import com.lifetracker.mobile.domain.model.TaskDifficulty
import com.lifetracker.mobile.domain.model.TaskType
import com.lifetracker.mobile.core.serialization.JsonDefaults
import com.lifetracker.mobile.domain.model.ChecklistItem
import com.lifetracker.mobile.ui.model.ChecklistItemUi
import com.lifetracker.mobile.ui.model.UiDifficulty
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
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
        TaskType.Daily   -> UiTaskType.Daily
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
    repeatPatternText = repeatPattern?.toRepeatText(),
    checklistItems = parseChecklist(checklistJson),
    rewardText = "+$baseXp XP +$baseGold Gold",
    penaltyText = "-$hpPenalty HP -$goldPenalty Gold",
    streakText = streak?.takeIf { it.currentDays > 0 }
        ?.let { "\uD83D\uDD25 ${it.currentDays} days (+${it.bonusXpPercent}%)" },
    isPendingSync = id < 0,
)

fun TaskType.toUi(): UiTaskType = when (this) {
    TaskType.Habit -> UiTaskType.Habit
    TaskType.OneTime -> UiTaskType.OneTime
    TaskType.Daily -> UiTaskType.Daily
    TaskType.Unknown -> UiTaskType.Unknown
}

fun UiTaskType.toDomain(): TaskType = when (this) {
    UiTaskType.Habit   -> TaskType.Habit
    UiTaskType.OneTime -> TaskType.OneTime
    UiTaskType.Daily   -> TaskType.Daily
    UiTaskType.Unknown -> TaskType.Unknown
}

fun UiDifficulty.toDomain(): TaskDifficulty = when (this) {
    UiDifficulty.Easy   -> TaskDifficulty.Easy
    UiDifficulty.Medium -> TaskDifficulty.Medium
    UiDifficulty.Hard   -> TaskDifficulty.Hard
    UiDifficulty.Epic   -> TaskDifficulty.Epic
}

private val shortDateFormatter: DateTimeFormatter by lazy {
    DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(Locale.getDefault())
}

private fun Instant.toDisplayDate(): String {
    val javaDate = this.toLocalDateTime(TimeZone.currentSystemDefault()).date.toJavaLocalDate()
    return shortDateFormatter.format(javaDate)
}

private fun String.toRepeatText(): String {
    val parts = split(":")
    val freq = parts.getOrNull(0) ?: return this
    val n = parts.getOrNull(1)?.toIntOrNull() ?: 1
    return when (freq) {
        "DAILY" -> if (n == 1) "Every day" else "Every $n days"
        "WEEKLY" -> if (n == 1) "Every week" else "Every $n weeks"
        "MONTHLY" -> if (n == 1) "Every month" else "Every $n months"
        "YEARLY" -> if (n == 1) "Every year" else "Every $n years"
        else -> freq
    }
}

private fun parseChecklist(checklistJson: String?): ImmutableList<ChecklistItemUi> {
    if (checklistJson.isNullOrBlank()) return persistentListOf()
    return runCatching {
        JsonDefaults.decodeFromString<List<ChecklistItem>>(checklistJson)
    }
        .onFailure { timber.log.Timber.w(it, "parseChecklist: failed to parse JSON") }
        .getOrDefault(emptyList())
        .map { ChecklistItemUi(id = it.id, text = it.text, isCompleted = it.isCompleted) }
        .toImmutableList()
}