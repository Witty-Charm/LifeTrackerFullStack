package com.lifetracker.mobile.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lifetracker.mobile.domain.model.HabitPolarity
import com.lifetracker.mobile.domain.model.TaskDifficulty
import com.lifetracker.mobile.domain.model.TaskType

@Entity(
    tableName = "tasks",
    indices = [
        Index("heroId"),
        Index("pendingSync"),
    ],
)
data class TaskEntity(
    @PrimaryKey val id: Int,
    val heroId: Int,
    val title: String,
    val description: String,
    val type: TaskType,
    val difficulty: TaskDifficulty,
    val habitPolarity: HabitPolarity? = null,
    val isCompleted: Boolean,
    val isCheckedToday: Boolean = false,
    val isActive: Boolean,
    val dueDate: Long?,
    val repeatPattern: String?,
    val checklistJson: String?,
    val remindersJson: String?,
    val isOverdue: Boolean,
    val completionCount: Int,
    val failCount: Int,
    val lastCompletedAt: Long?,
    val overdueProcessedAt: Long?,
    val baseXp: Int,
    val baseGold: Int,
    val hpPenalty: Int,
    val goldPenalty: Int,
    val streakCurrentDays: Int?,
    val streakBonusXpPercent: Int?,
    val streakMultiplier: Double?,
    val streakIsFrozen: Boolean?,
    val streakIsShieldActive: Boolean?,
    val pendingSync: Boolean = false,
    val syncError: String? = null,
)
