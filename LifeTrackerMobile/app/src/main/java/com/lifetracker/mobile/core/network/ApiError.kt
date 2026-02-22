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
)