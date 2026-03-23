package com.lifetracker.mobile.core.theme

import androidx.appcompat.app.AppCompatDelegate
import com.lifetracker.mobile.domain.model.ThemeMode
import com.lifetracker.mobile.domain.usecase.settings.ThemeSettingsUseCases
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ThemeController(
    private val themeSettingsUseCases: ThemeSettingsUseCases
) {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Default)
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    suspend fun applyInitialTheme() {
        val initialMode = themeSettingsUseCases.observeThemeMode().first()
        _themeMode.value = initialMode
        withContext(Dispatchers.Main.immediate) {
            AppCompatDelegate.setDefaultNightMode(initialMode.toNightMode())
        }
    }

    fun startObserving() {
        scope.launch {
            themeSettingsUseCases.observeThemeMode()
                .distinctUntilChanged()
                .collect { mode ->
                    withContext(Dispatchers.Main.immediate) {
                        AppCompatDelegate.setDefaultNightMode(mode.toNightMode())
                    }
                    _themeMode.value = mode
                }
        }
    }

    fun stopObserving() {
        job.cancel()
    }
}
