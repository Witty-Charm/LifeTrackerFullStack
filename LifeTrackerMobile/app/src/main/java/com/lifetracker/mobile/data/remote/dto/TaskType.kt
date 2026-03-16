package com.lifetracker.mobile.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
enum class TaskType {
    Habit,
    OneTime,
    Daily,
    Unknown,
}

@Serializable
enum class TaskDifficulty {
    Easy,
    Medium,
    Hard,
    Epic,
    Unknown,
}

