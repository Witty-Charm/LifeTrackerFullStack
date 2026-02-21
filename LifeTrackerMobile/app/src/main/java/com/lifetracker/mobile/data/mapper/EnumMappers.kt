package com.lifetracker.mobile.data.mapper

import com.lifetracker.mobile.data.remote.dto.TaskDifficulty as TaskDifficultyDto
import com.lifetracker.mobile.data.remote.dto.TaskType as TaskTypeDto
import com.lifetracker.mobile.domain.model.TaskDifficulty
import com.lifetracker.mobile.domain.model.TaskType

fun TaskTypeDto.toDomain(): TaskType = when (this) {
    TaskTypeDto.Habit   -> TaskType.Habit
    TaskTypeDto.OneTime -> TaskType.OneTime
    TaskTypeDto.Unknown -> TaskType.Unknown
}

fun TaskDifficultyDto.toDomain(): TaskDifficulty = when (this) {
    TaskDifficultyDto.Easy   -> TaskDifficulty.Easy
    TaskDifficultyDto.Medium -> TaskDifficulty.Medium
    TaskDifficultyDto.Hard   -> TaskDifficulty.Hard
    TaskDifficultyDto.Epic   -> TaskDifficulty.Epic
    TaskDifficultyDto.Unknown -> TaskDifficulty.Unknown
}

fun TaskType.toDto(): TaskTypeDto = when (this) {
    TaskType.Habit   -> TaskTypeDto.Habit
    TaskType.OneTime -> TaskTypeDto.OneTime
    TaskType.Unknown -> TaskTypeDto.Unknown
}

fun TaskDifficulty.toDto(): TaskDifficultyDto = when (this) {
    TaskDifficulty.Easy   -> TaskDifficultyDto.Easy
    TaskDifficulty.Medium -> TaskDifficultyDto.Medium
    TaskDifficulty.Hard   -> TaskDifficultyDto.Hard
    TaskDifficulty.Epic   -> TaskDifficultyDto.Epic
    TaskDifficulty.Unknown -> TaskDifficultyDto.Unknown
}