package com.lifetracker.mobile.ui.model

import androidx.compose.runtime.Immutable
import kotlin.time.Instant
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

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
    val isDead: Boolean,
    val isInRecovery: Boolean,
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
)

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
    data class Generic(val message: String) : UiError
}

sealed interface UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent
    data object TaskCreated : UiEvent
    data class TaskCompleted(val message: String) : UiEvent
    data class TaskFailed(val message: String) : UiEvent
    data class HeroRespawned(
        val message: String,
        val recoveryEndsAt: Instant?,
    ) : UiEvent
    data class HeroHealed(val message: String) : UiEvent
}