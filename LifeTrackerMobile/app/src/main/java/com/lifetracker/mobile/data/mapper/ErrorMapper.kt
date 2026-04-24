package com.lifetracker.mobile.data.mapper

import com.lifetracker.mobile.core.network.ApiError
import com.lifetracker.mobile.domain.model.GameError

private object GameErrorCodes {
    const val HERO_DEAD = "HERO_DEAD"
    const val HERO_ALREADY_DEAD = "HERO_ALREADY_DEAD"
    const val COMPLETION_LIMIT_REACHED = "COMPLETION_LIMIT_REACHED"
}
fun ApiError.toDomain(): GameError = when (errorCode) {
    GameErrorCodes.HERO_DEAD,
    GameErrorCodes.HERO_ALREADY_DEAD -> GameError.HeroDead

    GameErrorCodes.COMPLETION_LIMIT_REACHED -> GameError.DailyLimit(
        completions = completions ?: 0,
        max = maxCompletions ?: 0,
        resetAt = resetTime,
    )

    else -> when {
        !errors.isNullOrEmpty() -> GameError.Validation(errors)
        else -> GameError.Unknown(
            message ?: error ?: title ?: "Unknown error"
        )
    }
}
