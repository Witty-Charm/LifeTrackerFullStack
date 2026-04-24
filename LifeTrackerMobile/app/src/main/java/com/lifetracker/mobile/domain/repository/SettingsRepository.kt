package com.lifetracker.mobile.domain.repository

import com.lifetracker.mobile.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val themeModeFlow: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
    fun getOrCreateDeviceIdBlocking(): String
}
