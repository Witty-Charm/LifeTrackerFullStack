package com.lifetracker.mobile.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "heroes")
data class HeroEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val level: Int,
    val currentXp: Long,
    val maxXp: Long,
    val currentHp: Int,
    val maxHp: Int,
    val gold: Int,
    val isDead: Boolean,
    val deathCount: Int,
    val isInRecovery: Boolean,
    val recoveryMultiplier: Double,
    val xpBoostPercent: Int = 0,
    val xpBoostTasksRemaining: Int = 0,
    val dailyCompletions: Int,
    val dailyCompletionsMax: Int,
    val pendingSync: Boolean = false
)