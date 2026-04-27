package com.lifetracker.mobile.ui.mapper

import com.lifetracker.mobile.core.serialization.JsonDefaults
import com.lifetracker.mobile.domain.model.ChecklistItem
import com.lifetracker.mobile.domain.model.GameTaskDomain
import com.lifetracker.mobile.domain.model.HabitPolarity
import com.lifetracker.mobile.domain.model.habitResetPeriod
import com.lifetracker.mobile.domain.model.HeroDomain
import com.lifetracker.mobile.domain.model.HeroStatsDomain
import com.lifetracker.mobile.domain.model.InventoryItemDomain
import com.lifetracker.mobile.domain.model.ShopItemDomain
import com.lifetracker.mobile.domain.model.TaskDifficulty
import com.lifetracker.mobile.domain.model.TaskType
import com.lifetracker.mobile.ui.model.ChecklistItemUi
import com.lifetracker.mobile.ui.model.HeroStatusBadge
import com.lifetracker.mobile.ui.model.HeroUi
import com.lifetracker.mobile.ui.model.InventoryItemUi
import com.lifetracker.mobile.ui.model.ShopItemUi
import com.lifetracker.mobile.ui.model.StatsScreenState
import com.lifetracker.mobile.ui.model.TaskUi
import com.lifetracker.mobile.ui.model.UiDifficulty
import com.lifetracker.mobile.ui.model.UiTaskType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.time.Instant

fun HeroDomain.toUi(): HeroUi =
    HeroUi(
        id = id,
        name = name,
        level = level,
        xpText = "%,d / %,d XP".format(currentXp, maxXp),
        xpProgress = xpProgress,
        hpText = "$currentHp / $maxHp HP",
        hpProgress = hpProgress,
        goldText = "%,d Gold".format(gold),
        gold = gold,
        currentHp = currentHp,
        maxHp = maxHp,
        isDead = isDead,
        isInRecovery = isInRecovery,
        xpBoostPercent = xpBoostPercent,
        xpBoostTasksRemaining = xpBoostTasksRemaining,
        dailyText = "$dailyCompletions / $dailyCompletionsMax tasks today",
        dailyProgress = dailyProgress,
        statusBadge =
            when {
                isDead -> HeroStatusBadge.Dead
                isInRecovery -> HeroStatusBadge.Recovery
                else -> HeroStatusBadge.Alive
            },
    )

fun GameTaskDomain.toUi(): TaskUi =
    TaskUi(
        id = id,
        title = title,
        description = description,
        type =
            when (type) {
                TaskType.Habit -> UiTaskType.Habit
                TaskType.OneTime -> UiTaskType.OneTime
                TaskType.Daily -> UiTaskType.Daily
                TaskType.Unknown -> UiTaskType.Unknown
            },
        habitPolarity = if (type == TaskType.Habit) habitPolarity else HabitPolarity.Both,
        difficulty = difficulty.toUi(),
        difficultyLabel =
            when (difficulty) {
                TaskDifficulty.Easy -> "Easy"
                TaskDifficulty.Medium -> "Medium"
                TaskDifficulty.Hard -> "Hard"
                TaskDifficulty.Epic -> "Epic"
                TaskDifficulty.Unknown -> "Unknown"
            },
        difficultyColor =
            when (difficulty) {
                TaskDifficulty.Easy -> 0xFF4CAF50
                TaskDifficulty.Medium -> 0xFFFFC107
                TaskDifficulty.Hard -> 0xFFFF5722
                TaskDifficulty.Epic -> 0xFF9C27B0
                TaskDifficulty.Unknown -> 0xFF9E9E9E
            },
        isCompleted = isCompleted,
        isCheckedToday = isCheckedToday,
        isOverdue = isOverdue,
        dueDateText = if (type == TaskType.Habit) null else dueDate?.toDisplayDate(),
        repeatPatternText =
            when (type) {
                TaskType.Habit -> habitResetPeriod?.let { "Reset: ${it.label}" }
                else -> repeatPattern?.toRepeatText()
            },
        checklistItems = parseChecklist(checklistJson),
        rewardText = "+$baseXp XP +$baseGold Gold",
        penaltyText = "-$hpPenalty HP -$goldPenalty Gold",
        streakText =
            streak
                ?.takeIf { it.currentDays > 0 }
                ?.let { "\uD83D\uDD25 ${it.currentDays} days (+${it.bonusXpPercent}%)" },
        isPendingSync = pendingSync,
        syncError = syncError,
        pendingAction = null,
        actionError = null,
        isShieldActive = streak?.isShieldActive ?: false,
        isScheduledToday = isScheduledToday,
        nextScheduledHint =
            if (type == TaskType.Daily && !isScheduledToday) {
                nextScheduledLocalDate?.toScheduledHint()
            } else {
                null
            },
    )

private fun String.toScheduledHint(): String? {
    val date = runCatching { java.time.LocalDate.parse(this) }.getOrNull() ?: return null
    return "Next: ${shortDateFormatter.format(date)}"
}

fun TaskType.toUi(): UiTaskType =
    when (this) {
        TaskType.Habit -> UiTaskType.Habit
        TaskType.OneTime -> UiTaskType.OneTime
        TaskType.Daily -> UiTaskType.Daily
        TaskType.Unknown -> UiTaskType.Unknown
    }

fun UiTaskType.toDomain(): TaskType =
    when (this) {
        UiTaskType.Habit -> TaskType.Habit
        UiTaskType.OneTime -> TaskType.OneTime
        UiTaskType.Daily -> TaskType.Daily
        UiTaskType.Unknown -> TaskType.Unknown
    }

fun UiDifficulty.toDomain(): TaskDifficulty =
    when (this) {
        UiDifficulty.Easy -> TaskDifficulty.Easy
        UiDifficulty.Medium -> TaskDifficulty.Medium
        UiDifficulty.Hard -> TaskDifficulty.Hard
        UiDifficulty.Epic -> TaskDifficulty.Epic
    }

fun TaskDifficulty.toUi(): UiDifficulty =
    when (this) {
        TaskDifficulty.Easy -> UiDifficulty.Easy
        TaskDifficulty.Medium -> UiDifficulty.Medium
        TaskDifficulty.Hard -> UiDifficulty.Hard
        TaskDifficulty.Epic -> UiDifficulty.Epic
        TaskDifficulty.Unknown -> UiDifficulty.Easy
    }

private val shortDateFormatter: DateTimeFormatter by lazy {
    DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(Locale.getDefault())
}

private val mediumDateFormatter: DateTimeFormatter by lazy {
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())
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
        "WEEKLY", "MONTHLY", "YEARLY" -> "Legacy schedule"
        else -> freq
    }
}

private fun parseChecklist(checklistJson: String?): ImmutableList<ChecklistItemUi> {
    if (checklistJson.isNullOrBlank()) return persistentListOf()
    return runCatching {
        JsonDefaults.decodeFromString<List<ChecklistItem>>(checklistJson)
    }.onFailure { timber.log.Timber.w(it, "parseChecklist: failed to parse JSON") }
        .getOrDefault(emptyList())
        .map { ChecklistItemUi(id = it.id, text = it.text, isCompleted = it.isCompleted) }
        .toImmutableList()
}

fun HeroStatsDomain.toStatsUiState(tasks: List<GameTaskDomain>): StatsScreenState =
    StatsScreenState(
        heroName = name,
        level = level,
        xpText = "%,d / %,d XP".format(currentXp, xpForNextLevel),
        hpText = "$currentHp / $maxHp HP",
        goldText = "%,d Gold".format(gold),
        totalXpEarnedText = "%,d".format(totalXpEarned),
        totalGoldEarnedText = "%,d".format(totalGoldEarned),
        totalGoldSpentText = "%,d".format(totalGoldSpent),
        deathCount = deathCount,
        activeStreaks = activeStreaks,
        longestStreak = longestStreak,
        dailyProgressText = "$dailyCompletions / $dailyCompletionsMax tasks today",
        completedCount = tasks.sumOf { it.completionCount },
        failedCount = tasks.sumOf { it.failCount },
        overdueCount = tasks.count { it.isOverdue },
        dailyCount = tasks.count { it.type == TaskType.Daily },
        oneTimeCount = tasks.count { it.type == TaskType.OneTime },
        habitCount = tasks.count { it.type == TaskType.Habit },
    )

fun ShopItemDomain.toUi(heroGold: Int): ShopItemUi =
    ShopItemUi(
        id = id,
        name = name,
        description = description,
        cost = price,
        itemType = itemType,
        effectValue = effectValue,
        canAfford = heroGold >= price,
    )

fun InventoryItemDomain.toUi(heroGold: Int): InventoryItemUi =
    InventoryItemUi(
        purchaseId = purchaseId,
        item = item.toUi(heroGold),
        purchasedAt =
            purchasedAt
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date
                .toJavaLocalDate()
                .format(mediumDateFormatter)
    )
