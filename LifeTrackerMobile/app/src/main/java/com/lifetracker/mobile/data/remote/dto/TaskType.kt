package com.lifetracker.mobile.data.remote.dto

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

@Serializable
enum class TaskType(val value: Int) {
    Habit(1),
    OneTime(2),
    Unknown(-1);

    companion object {
        fun fromValue(value: Int): TaskType =
            entries.firstOrNull { it.value == value } ?: Unknown
    }
}

@Serializable
enum class TaskDifficulty(val value: Int) {
    Easy(1),
    Medium(2),
    Hard(3),
    Epic(4),
    Unknown(-1);

    companion object {
        fun fromValue(value: Int): TaskDifficulty =
            entries.firstOrNull { it.value == value } ?: Unknown
    }
}

