package com.lifetracker.mobile.ui.mapper

import android.content.Context
import com.lifetracker.mobile.R
import com.lifetracker.mobile.domain.model.GameError
import com.lifetracker.mobile.ui.model.UiError

fun GameError.toUiError(): UiError = when (this) {
    is GameError.HeroDead -> UiError.HeroDead
    is GameError.DailyLimit -> UiError.DailyLimitReached(
        completions = completions,
        max = max,
        resetTime = resetAt,
    )
    is GameError.Validation -> UiError.Validation(fieldErrors = fields)
    is GameError.Network -> UiError.Network
    is GameError.Unknown -> UiError.Generic(message)
}

fun UiError.toMessage(context: Context): String = when (this) {
    is UiError.HeroDead -> context.getString(R.string.error_hero_dead)
    is UiError.Network -> context.getString(R.string.error_network)
    is UiError.DailyLimitReached -> context.getString(
        R.string.error_daily_limit, completions, max
    )
    is UiError.Validation -> fieldErrors.values.flatten().firstOrNull()
        ?: context.getString(R.string.error_validation)
    is UiError.Generic -> message
}