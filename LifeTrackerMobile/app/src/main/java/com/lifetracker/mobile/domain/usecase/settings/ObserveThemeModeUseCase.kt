package com.lifetracker.mobile.domain.usecase.settings

import com.lifetracker.mobile.domain.model.ThemeMode
import com.lifetracker.mobile.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class ObserveThemeModeUseCase(
    private val repo: SettingsRepository
) {
    operator fun invoke(): Flow<ThemeMode> = repo.themeModeFlow
}
