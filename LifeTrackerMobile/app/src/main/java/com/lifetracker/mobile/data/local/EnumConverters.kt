package com.lifetracker.mobile.data.local

import androidx.room.TypeConverter
import com.lifetracker.mobile.domain.model.TaskDifficulty
import com.lifetracker.mobile.domain.model.TaskType

class EnumConverters {

    @TypeConverter
    fun fromTaskType(value: String): TaskType =
        TaskType.entries.find { it.name == value } ?: TaskType.Unknown

    @TypeConverter
    fun toTaskType(type: TaskType): String = type.name

    @TypeConverter
    fun fromTaskDifficulty(value: String): TaskDifficulty =
        TaskDifficulty.entries.find { it.name == value } ?: TaskDifficulty.Unknown

    @TypeConverter
    fun toTaskDifficulty(difficulty: TaskDifficulty): String = difficulty.name
}