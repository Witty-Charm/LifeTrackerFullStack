package com.lifetracker.mobile.data.mapper

import com.lifetracker.mobile.core.network.ApiError
import com.lifetracker.mobile.domain.model.GameError

private object GameErrorCodes {
    const val HERO_DEAD = "HERO_DEAD"
    const val HERO_ALREADY_DEAD = "HERO_ALREADY_DEAD"
    const val DAILY_LIMIT_REACHED = "DAILY_LIMIT_REACHED"
}
fun ApiError.toDomain(): GameError = when (errorCode) {
    GameErrorCodes.HERO_DEAD,
    GameErrorCodes.HERO_ALREADY_DEAD -> GameError.HeroDead

    GameErrorCodes.DAILY_LIMIT_REACHED -> GameError.DailyLimit(
        completions = dailyCompletions ?: 0,
        max = maxDailyCompletions ?: 0,
        resetAt = resetTime,
    )

    else -> when {
        !errors.isNullOrEmpty() -> GameError.Validation(errors)
        else -> GameError.Unknown(
            message ?: error ?: title ?: "Unknown error"
        )
    }
}

