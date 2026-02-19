package com.lifetracker.mobile.ui.model

import com.lifetracker.mobile.domain.model.HeroDomain
import com.lifetracker.mobile.domain.model.TaskCompletionResult
import com.lifetracker.mobile.domain.model.TaskDifficulty
import com.lifetracker.mobile.domain.model.TaskFailureResult
import com.lifetracker.mobile.domain.model.TaskType
import kotlin.time.Instant

data class HeroScreenState(
    val heroDomain: HeroDomain? = null,
    val hero: HeroUi? = null,
    val tasks: List<TaskUi> = emptyList(),
    val isLoading: Boolean = false,
    val isActionLoading: Boolean = false,
    val needsHeroCreation: Boolean = false,
    val criticalError: UiError? = null,
    val actionError: UiError? = null,
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
    val type: TaskType,
    val difficulty: TaskDifficulty,
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
    data class ShowSnackbar(val message: String) : UiEvent
    data class TaskCompleted(val result: TaskCompletionResult) : UiEvent
    data class TaskFailed(val result: TaskFailureResult) : UiEvent
    data class HeroRespawned(
        val message: String,
        val recoveryEndsAt: Instant?,
    ) : UiEvent
    data class HeroHealed(val message: String) : UiEvent
}