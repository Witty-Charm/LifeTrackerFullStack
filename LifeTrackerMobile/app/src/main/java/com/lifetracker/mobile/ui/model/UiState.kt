package com.lifetracker.mobile.ui.model

import androidx.compose.runtime.Immutable
import com.lifetracker.mobile.domain.model.HabitPolarity
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlin.time.Clock
import kotlin.time.Instant

@Immutable
data class HeroScreenState(
    val hero: HeroUi? = null,
    val tasks: ImmutableList<TaskUi> = persistentListOf(),
    val isLoading: Boolean = false,
    val loadingActions: ImmutableSet<String> = persistentSetOf(),
    val needsHeroCreation: Boolean = false,
    val criticalError: UiError? = null,
    val actionError: UiError? = null,
)

val HeroScreenState.isAnyActionLoading get() = loadingActions.isNotEmpty()
val HeroScreenState.isHealLoading get() = "hero_heal" in loadingActions
val HeroScreenState.isRespawnLoading get() = "hero_respawn" in loadingActions

fun HeroScreenState.isTaskLoading(taskId: Int) =
    "task_complete_$taskId" in loadingActions ||
        "task_fail_$taskId" in loadingActions ||
        "task_delete_$taskId" in loadingActions

@Immutable
data class HeroUi(
    val id: Int,
    val name: String,
    val level: Int,
    val xpText: String,
    val xpProgress: Float,
    val hpText: String,
    val hpProgress: Float,
    val goldText: String,
    val gold: Int,
    val currentHp: Int,
    val maxHp: Int,
    val isDead: Boolean,
    val isInRecovery: Boolean,
    val xpBoostPercent: Int = 0,
    val xpBoostTasksRemaining: Int = 0,
    val dailyText: String,
    val dailyProgress: Float,
    val statusBadge: HeroStatusBadge,
)

enum class HeroStatusBadge { Alive, Recovery, Dead }

enum class UiTaskType { Habit, OneTime, Daily, Unknown }

enum class UiDifficulty { Easy, Medium, Hard, Epic }

@Immutable
data class TaskUi(
    val id: Int,
    val title: String,
    val description: String,
    val type: UiTaskType,
    val habitPolarity: HabitPolarity = HabitPolarity.Both,
    val difficultyLabel: String,
    val difficultyColor: Long,
    val isCompleted: Boolean,
    val isOverdue: Boolean,
    val dueDateText: String?,
    val repeatPatternText: String? = null,
    val checklistItems: ImmutableList<ChecklistItemUi> = persistentListOf(),
    val rewardText: String,
    val penaltyText: String,
    val streakText: String?,
    val isPendingSync: Boolean,
    val syncError: String? = null,
    val shieldExpiresAtUtc: Instant? = null,
    val isShieldActive: Boolean = false,
) {
    val shieldCountdownText: String?
        get() {
            val expiry = shieldExpiresAtUtc ?: return null
            if (!isShieldActive) return null

            val now = Clock.System.now()
            if (expiry <= now) return null

            val duration = expiry - now
            val hours = duration.inWholeHours
            val minutes = (duration.inWholeMinutes % 60)
            return String.format("%02d:%02d left", hours, minutes)
        }

    val showsPositiveAction: Boolean
        get() = true

    val showsNegativeAction: Boolean
        get() = true

    val positiveActionEnabled: Boolean
        get() = type != UiTaskType.Habit || habitPolarity != HabitPolarity.Negative

    val negativeActionEnabled: Boolean
        get() = type != UiTaskType.Habit || habitPolarity != HabitPolarity.Positive
}

@Immutable
data class ChecklistItemUi(
    val id: String,
    val text: String,
    val isCompleted: Boolean,
)

sealed interface UiError {
    data object HeroDead : UiError

    data class DailyLimitReached(
        val completions: Int,
        val max: Int,
        val resetTime: String?,
    ) : UiError

    data class Validation(
        val fieldErrors: Map<String, List<String>>,
    ) : UiError

    data object Network : UiError

    data class Generic(
        val message: String,
    ) : UiError
}

sealed interface TaskActionFeedback {
    data class Completed(
        val xpGained: Long,
        val goldGained: Int,
        val leveledUp: Boolean,
        val newLevel: Int?,
    ) : TaskActionFeedback

    data class Failed(
        val hpLost: Int,
        val goldLost: Int,
        val shieldAbsorbed: Boolean,
    ) : TaskActionFeedback
}

@Immutable
data class AchievementUi(
    val key: String,
    val title: String,
    val description: String,
    val category: String,
    val threshold: Int,
    val sortOrder: Int,
    val goldReward: Int,
    val unlocked: Boolean,
    val unlockedAt: Instant?,
)

@Immutable
data class AchievementsScreenState(
    val achievements: ImmutableList<AchievementUi> = persistentListOf(),
    val isLoading: Boolean = false,
    val actionError: UiError? = null,
)

sealed interface UiEvent {
    data class ShowSnackbar(
        val message: String,
    ) : UiEvent

    data class TaskCreated(
        val type: UiTaskType,
    ) : UiEvent

    data class TaskAction(
        val feedback: TaskActionFeedback,
    ) : UiEvent

    data class HeroRespawned(
        val message: String,
        val recoveryEndsAt: Instant?,
    ) : UiEvent

    data class HeroHealed(
        val message: String,
    ) : UiEvent

    data class HeroGoldUpdated(
        val newGold: Int,
    ) : UiEvent

    data class HeroHpUpdated(
        val newHp: Int,
        val maxHp: Int,
    ) : UiEvent

    data class HeroXpBoostUpdated(
        val percent: Int,
        val tasksRemaining: Int,
    ) : UiEvent

    data class HeroRecoveryUpdated(
        val isInRecovery: Boolean,
        val recoveryMultiplier: Double,
    ) : UiEvent
}

@Immutable
data class ShopScreenState(
    val items: ImmutableList<ShopItemUi> = persistentListOf(),
    val inventory: ImmutableList<InventoryItemUi> = persistentListOf(),
    val isLoadingItems: Boolean = false,
    val isLoadingInventory: Boolean = false,
    val loadingActions: ImmutableSet<String> = persistentSetOf(),
    val actionError: UiError? = null,
    val showInventory: Boolean = false,
)

fun ShopScreenState.isBuyLoading(itemId: Int) = "buy_$itemId" in loadingActions
