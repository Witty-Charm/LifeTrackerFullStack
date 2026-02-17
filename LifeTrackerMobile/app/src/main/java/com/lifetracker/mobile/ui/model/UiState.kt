package com.lifetracker.mobile.ui.model

data class HeroScreenState(
    val hero: HeroUi? = null,
    val tasks: List<TaskUi> = emptyList(),
    val isLoading: Boolean = false,
    val error: UiError? = null,
    val event: UiEvent? = null,
)

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

data class TaskUi(
    val id: Int,
    val title: String,
    val description: String,
    val typeLabel: String,
    val difficultyLabel: String,
    val difficultyColor: Long,
    val isCompleted: Boolean,
    val isOverdue: Boolean,
    val dueDateText: String?,
    val rewardText: String,
    val penaltyText: String,
    val streakText: String?,
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
    data class TaskCompleted(
        val taskTitle: String,
        val xpGained: Long,
        val goldGained: Int,
        val leveledUp: Boolean,
        val newLevel: Int,
        val message: String,
    ) : UiEvent

    data class TaskFailed(
        val taskTitle: String,
        val heroDied: Boolean,
        val message: String,
    ) : UiEvent

    data class HeroRespawned(val message: String) : UiEvent
    data class HeroHealed(val message: String) : UiEvent
    data class ShowSnackbar(val text: String) : UiEvent
}