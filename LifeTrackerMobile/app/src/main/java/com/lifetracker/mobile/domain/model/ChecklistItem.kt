package com.lifetracker.mobile.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ChecklistItem(
    val id: String,
    val text: String,
    val isCompleted: Boolean = false,
)
