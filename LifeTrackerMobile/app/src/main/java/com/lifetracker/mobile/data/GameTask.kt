package com.lifetracker.mobile.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "tasks")
data class GameTask(
    @PrimaryKey(autoGenerate = true)
    val localId: Long = 0,

    @SerializedName("id")
    val serverId: Long? = null,

    val title: String,
    val description: String? = null,
    @SerializedName("rewardXp")
    val rewardXp: Int,
    val isCompleted: Boolean = false,
    val isSynced: Boolean = false
)

