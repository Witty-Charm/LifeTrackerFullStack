package com.lifetracker.mobile.core.network

import kotlinx.serialization.Serializable

@Serializable
data class ApiError(
    val error: String? = null,
    val message: String? = null,
    val title: String? = null,
    val status: Int? = null,
    val errors: Map<String, List<String>>? = null,
    val dailyCompletions: Int? = null,
    val maxDailyCompletions: Int? = null,
    val resetTime: String? = null,
) {

    val displayMessage: String
        get() = message
            ?: error
            ?: title
            ?: errors?.values?.flatten()?.firstOrNull()
            ?: "Unknown error"

    val isHeroDead: Boolean get() = error == "Hero is dead"
    val isHeroAlreadyDead: Boolean get() = error == "Hero is already dead"
    val isDailyLimitReached: Boolean get() = error == "Daily limit reached"
    val isValidationError: Boolean get() = !errors.isNullOrEmpty()
}