package com.lifetracker.mobile.data.mapper

import com.lifetracker.mobile.core.network.ApiError
import com.lifetracker.mobile.domain.model.GameError

fun ApiError.toDomain(): GameError = when (errorCode) {
    ApiError.HERO_DEAD,
    ApiError.HERO_ALREADY_DEAD -> GameError.HeroDead

    ApiError.DAILY_LIMIT_REACHED -> GameError.DailyLimit(
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

