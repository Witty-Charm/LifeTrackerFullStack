package com.lifetracker.mobile.core.network

import kotlinx.serialization.Serializable

@Serializable
data class ApiError(
    val errorCode: String? = null,
    val error: String? = null,
    val message: String? = null,
    val title: String? = null,
    val status: Int? = null,
    val errors: Map<String, List<String>>? = null,
    val dailyCompletions: Int? = null,
    val maxDailyCompletions: Int? = null,
    val resetTime: String? = null,
) {
    companion object {
        const val HERO_DEAD = "HERO_DEAD"
        const val HERO_ALREADY_DEAD = "HERO_ALREADY_DEAD"
        const val DAILY_LIMIT_REACHED = "DAILY_LIMIT_REACHED"
    }
    val displayMessage: String
        get() = message
            ?: error
            ?: title
            ?: errors?.values?.flatten()?.firstOrNull()
            ?: "Unknown error"
}