package com.lifetracker.mobile.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.lifetracker.mobile.domain.model.ThemeMode
import com.lifetracker.mobile.domain.repository.SettingsRepository
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber

class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {
    override val themeModeFlow: Flow<ThemeMode> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            ThemeMode.fromStoredValue(preferences[themeModeKey])
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        try {
            dataStore.edit { preferences ->
                preferences[themeModeKey] = mode.storageValue
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: IOException) {
            Timber.w(exception, "Failed to persist theme mode.")
        }
    }

    private companion object {
        val themeModeKey = stringPreferencesKey("theme_mode")
    }
}
