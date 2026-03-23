package com.lifetracker.mobile.domain.model

enum class ThemeMode(val storageValue: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    fun toNightMode(): Int = when (this) {
        SYSTEM -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        LIGHT -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        DARK -> androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
    }

    companion object {
        fun fromStoredValue(value: String?): ThemeMode = when (value) {
            LIGHT.storageValue -> LIGHT
            DARK.storageValue -> DARK
            SYSTEM.storageValue -> SYSTEM
            else -> SYSTEM
        }
    }
}
