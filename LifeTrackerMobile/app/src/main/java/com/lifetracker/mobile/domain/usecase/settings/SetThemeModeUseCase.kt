package com.lifetracker.mobile.domain.usecase.settings

import com.lifetracker.mobile.domain.model.ThemeMode
import com.lifetracker.mobile.domain.repository.SettingsRepository

class SetThemeModeUseCase(
    private val repo: SettingsRepository
) {
    suspend operator fun invoke(mode: ThemeMode) {
        repo.setThemeMode(mode)
    }
}
