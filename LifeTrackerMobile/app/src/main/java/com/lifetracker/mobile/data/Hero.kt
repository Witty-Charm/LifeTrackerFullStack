package com.lifetracker.mobile.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "heroes")
data class Hero(
    @PrimaryKey
    val id: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("level")
    val level: Int,

    @SerializedName("xp")
    val xp: Int,

    @SerializedName("maxXP")
    val experienceToNextLevel: Int,

    @SerializedName("hp")
    val hp: Int
) {
    fun getExperienceProgress(): Float {
        return if (experienceToNextLevel > 0) {
            xp.toFloat() / experienceToNextLevel.toFloat()
        } else {
            0f
        }
    }
}


