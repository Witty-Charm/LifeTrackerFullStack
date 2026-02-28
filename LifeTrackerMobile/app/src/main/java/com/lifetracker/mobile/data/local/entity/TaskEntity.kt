package com.lifetracker.mobile.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    indices = [
        Index("heroId"),
        Index("pendingSync"),
    ]
)
data class TaskEntity(
    @PrimaryKey val id: Int,
    val heroId: Int,
    val title: String,
    val description: String,
    val type: String,
    val difficulty: String,
    val isCompleted: Boolean,
    val isActive: Boolean,
    val dueDate: Long?,
    val isOverdue: Boolean,
    val completionCount: Int,
    val failCount: Int,
    val lastCompletedAt: Long?,
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
)