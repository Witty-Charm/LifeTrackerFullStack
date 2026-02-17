package com.lifetracker.mobile.domain.model

sealed class GameError {
    data object HeroDead : GameError()
    data class DailyLimit(
        val completions: Int,
        val max: Int,
        val resetAt: String?
    ) : GameError()
    data class Validation(val fields: Map<String, List<String>>) : GameError()
    data class Unknown(val message: String) : GameError()
}

