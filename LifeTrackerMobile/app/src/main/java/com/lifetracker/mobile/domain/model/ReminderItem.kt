package com.lifetracker.mobile.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ReminderItem(
    val id: String,
    val hour: Int,
    val minute: Int,
)
