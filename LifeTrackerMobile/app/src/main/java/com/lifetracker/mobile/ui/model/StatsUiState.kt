package com.lifetracker.mobile.ui.model

import androidx.compose.runtime.Immutable

@Immutable
data class StatsScreenState(
    val heroName: String = "",
    val level: Int = 0,
    val xpText: String = "",
    val hpText: String = "",
    val goldText: String = "",
    val totalXpEarnedText: String = "0",
    val totalGoldEarnedText: String = "0",
    val totalGoldSpentText: String = "0",
    val deathCount: Int = 0,
    val activeStreaks: Int = 0,
    val longestStreak: Int = 0,
    val dailyProgressText: String = "",
    val completedCount: Int = 0,
    val failedCount: Int = 0,
    val overdueCount: Int = 0,
    val dailyCount: Int = 0,
    val oneTimeCount: Int = 0,
    val habitCount: Int = 0,
    val isLoading: Boolean = false,
    val actionError: UiError? = null,
)
