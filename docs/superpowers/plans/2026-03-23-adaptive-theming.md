# Adaptive Theming Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add adaptive light/dark theming that follows system mode, persists ThemeMode, and is ready for a future manual toggle.

**Architecture:** Introduce a ThemeMode model stored in Preferences DataStore via a SettingsRepository + UseCases. Apply AppCompatDelegate synchronously on startup and react to flow changes. Compose theme receives ThemeMode and selects light/dark color schemes accordingly.

**Tech Stack:** Kotlin, Jetpack Compose (Material3), Koin, Preferences DataStore, AppCompatDelegate.

---

## File Structure & Responsibilities

**Create:**
- `app/src/main/java/com/lifetracker/mobile/domain/model/ThemeMode.kt` — ThemeMode enum + storage/mapping helpers.
- `app/src/main/java/com/lifetracker/mobile/domain/repository/SettingsRepository.kt` — interface for theme settings.
- `app/src/main/java/com/lifetracker/mobile/domain/usecase/settings/ObserveThemeModeUseCase.kt` — observe theme mode flow.
- `app/src/main/java/com/lifetracker/mobile/domain/usecase/settings/SetThemeModeUseCase.kt` — set theme mode.
- `app/src/main/java/com/lifetracker/mobile/domain/usecase/settings/ThemeSettingsUseCases.kt` — bundle use cases.
- `app/src/main/java/com/lifetracker/mobile/data/repository/SettingsRepositoryImpl.kt` — DataStore-backed implementation.
- `app/src/main/java/com/lifetracker/mobile/core/theme/ThemeController.kt` — sync apply + StateFlow exposure.

**Modify:**
- `gradle/libs.versions.toml` — add DataStore dependency.
- `app/build.gradle.kts` — include DataStore dependency.
- `app/src/main/java/com/lifetracker/mobile/di/AppModule.kt` — Koin bindings for DataStore, repository, use cases, controller.
- `app/src/main/java/com/lifetracker/mobile/App.kt` — synchronous apply + controller start.
- `app/src/main/java/com/lifetracker/mobile/MainActivity.kt` — collect ThemeMode flow and pass into theme.
- `app/src/main/java/com/lifetracker/mobile/ui/theme/Theme.kt` — add light scheme + ThemeMode parameter.
- `app/src/main/java/com/lifetracker/mobile/ui/theme/Color.kt` — define light palette colors.

---

### Task 1: Add DataStore dependency

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Update version catalog**

```toml
[versions]
# add
androidxDatastore = "1.1.1"

[libraries]
# add
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "androidxDatastore" }
```

- [ ] **Step 2: Add dependency to app module**

```kotlin
// app/build.gradle.kts dependencies
implementation(libs.androidx.datastore.preferences)
```

- [ ] **Step 3: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "build: add DataStore preferences"
```

---

### Task 2: Define ThemeMode model and mapping

**Files:**
- Create: `app/src/main/java/com/lifetracker/mobile/domain/model/ThemeMode.kt`

- [ ] **Step 1: Write minimal model**

```kotlin
package com.lifetracker.mobile.domain.model

import androidx.appcompat.app.AppCompatDelegate

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    val storageValue: String
        get() = name.lowercase()

    fun toNightMode(): Int = when (this) {
        SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
        DARK -> AppCompatDelegate.MODE_NIGHT_YES
    }

    companion object {
        fun fromStoredValue(value: String?): ThemeMode = when (value) {
            "light" -> LIGHT
            "dark" -> DARK
            else -> SYSTEM
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/lifetracker/mobile/domain/model/ThemeMode.kt
git commit -m "feat: add ThemeMode model"
```

---

### Task 3: SettingsRepository + use cases

**Files:**
- Create: `app/src/main/java/com/lifetracker/mobile/domain/repository/SettingsRepository.kt`
- Create: `app/src/main/java/com/lifetracker/mobile/domain/usecase/settings/ObserveThemeModeUseCase.kt`
- Create: `app/src/main/java/com/lifetracker/mobile/domain/usecase/settings/SetThemeModeUseCase.kt`
- Create: `app/src/main/java/com/lifetracker/mobile/domain/usecase/settings/ThemeSettingsUseCases.kt`

- [ ] **Step 1: Repository contract**

```kotlin
package com.lifetracker.mobile.domain.repository

import com.lifetracker.mobile.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val themeModeFlow: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}
```

- [ ] **Step 2: Use cases**

```kotlin
package com.lifetracker.mobile.domain.usecase.settings

import com.lifetracker.mobile.domain.model.ThemeMode
import com.lifetracker.mobile.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class ObserveThemeModeUseCase(
    private val repo: SettingsRepository
) {
    operator fun invoke(): Flow<ThemeMode> = repo.themeModeFlow
}

class SetThemeModeUseCase(
    private val repo: SettingsRepository
) {
    suspend operator fun invoke(mode: ThemeMode) {
        repo.setThemeMode(mode)
    }
}

data class ThemeSettingsUseCases(
    val observeThemeMode: ObserveThemeModeUseCase,
    val setThemeMode: SetThemeModeUseCase,
)
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lifetracker/mobile/domain/repository/SettingsRepository.kt \
       app/src/main/java/com/lifetracker/mobile/domain/usecase/settings/ObserveThemeModeUseCase.kt \
       app/src/main/java/com/lifetracker/mobile/domain/usecase/settings/SetThemeModeUseCase.kt \
       app/src/main/java/com/lifetracker/mobile/domain/usecase/settings/ThemeSettingsUseCases.kt
git commit -m "feat: add settings repository use cases"
```

---

### Task 4: DataStore implementation

**Files:**
- Create: `app/src/main/java/com/lifetracker/mobile/data/repository/SettingsRepositoryImpl.kt`

- [ ] **Step 1: Implement DataStore-backed repository**

```kotlin
package com.lifetracker.mobile.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.lifetracker.mobile.domain.model.ThemeMode
import com.lifetracker.mobile.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    private val themeModeKey = stringPreferencesKey("theme_mode")

    override val themeModeFlow: Flow<ThemeMode> = dataStore.data
        .map { prefs -> ThemeMode.fromStoredValue(prefs[themeModeKey]) }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[themeModeKey] = mode.storageValue
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/lifetracker/mobile/data/repository/SettingsRepositoryImpl.kt
git commit -m "feat: add DataStore settings repository"
```

---

### Task 5: ThemeController (apply + StateFlow exposure)

**Files:**
- Create: `app/src/main/java/com/lifetracker/mobile/core/theme/ThemeController.kt`

- [ ] **Step 1: Implement controller**

```kotlin
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

class ThemeController(
    private val useCases: ThemeSettingsUseCases
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    suspend fun applyInitialTheme() {
        val mode = useCases.observeThemeMode().first()
        _themeMode.value = mode
        AppCompatDelegate.setDefaultNightMode(mode.toNightMode())
    }

    fun startObserving() {
        scope.launch {
            useCases.observeThemeMode()
                .distinctUntilChanged()
                .collect { mode ->
                    _themeMode.value = mode
                    AppCompatDelegate.setDefaultNightMode(mode.toNightMode())
                }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/lifetracker/mobile/core/theme/ThemeController.kt
git commit -m "feat: add theme controller"
```

---

### Task 6: Wire DI in Koin

**Files:**
- Modify: `app/src/main/java/com/lifetracker/mobile/di/AppModule.kt`

- [ ] **Step 1: Add DataStore singleton + bindings**

```kotlin
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.lifetracker.mobile.core.theme.ThemeController
import com.lifetracker.mobile.data.repository.SettingsRepositoryImpl
import com.lifetracker.mobile.domain.repository.SettingsRepository
import com.lifetracker.mobile.domain.usecase.settings.*
```

```kotlin
single<DataStore<Preferences>> {
    PreferenceDataStoreFactory.create(
        produceFile = { androidContext().preferencesDataStoreFile("settings") }
    )
}

single<SettingsRepository> { SettingsRepositoryImpl(dataStore = get()) }

single {
    val repo: SettingsRepository = get()
    ThemeSettingsUseCases(
        observeThemeMode = ObserveThemeModeUseCase(repo),
        setThemeMode = SetThemeModeUseCase(repo),
    )
}

single { ThemeController(useCases = get()) }
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/lifetracker/mobile/di/AppModule.kt
git commit -m "feat: wire theme settings in DI"
```

---

### Task 7: Apply theme at startup

**Files:**
- Modify: `app/src/main/java/com/lifetracker/mobile/App.kt`

- [ ] **Step 1: Apply initial theme synchronously**

```kotlin
import kotlinx.coroutines.runBlocking
import com.lifetracker.mobile.core.theme.ThemeController
import org.koin.core.component.get
```

```kotlin
override fun onCreate() {
    super.onCreate()
    startKoin { /* existing */ }

    val themeController: ThemeController = get()
    runBlocking { themeController.applyInitialTheme() }
    themeController.startObserving()
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/lifetracker/mobile/App.kt
git commit -m "feat: apply theme on startup"
```

---

### Task 8: Compose theme integration

**Files:**
- Modify: `app/src/main/java/com/lifetracker/mobile/ui/theme/Color.kt`
- Modify: `app/src/main/java/com/lifetracker/mobile/ui/theme/Theme.kt`
- Modify: `app/src/main/java/com/lifetracker/mobile/MainActivity.kt`

- [ ] **Step 1: Add light palette colors**

```kotlin
val AppBackgroundLight = Color(0xFFF5F5FA)
val SurfaceLight = Color(0xFFFFFFFF)
val CardBackgroundLight = Color(0xFFFFFFFF)
val CardBorderLight = Color(0xFFE2E2F0)
val TextPrimaryLight = Color(0xFF1A1A2E)
val TextSecondaryLight = Color(0xFF5C5C70)
val OnGoldTextLight = Color(0xFF12121A)
```

- [ ] **Step 2: Update theme to accept ThemeMode**

```kotlin
import androidx.compose.foundation.isSystemInDarkTheme
import com.lifetracker.mobile.domain.model.ThemeMode
```

```kotlin
private val LightColorScheme = lightColorScheme(
    background = AppBackgroundLight,
    surface = CardBackgroundLight,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    onSurfaceVariant = TextSecondaryLight,
    primary = PurpleAccent,
    onPrimary = TextPrimary,
    secondary = GoldYellow,
    onSecondary = OnGoldTextLight,
    error = HealthRed,
    outline = PurpleBorder,
    surfaceVariant = CardBorderLight
)
```

```kotlin
@Composable
fun LifeTrackerMobileTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    // status bar appearance: darkTheme == false => light status bars
    // set isAppearanceLightStatusBars = !darkTheme
}
```

- [ ] **Step 3: Pass ThemeMode from Activity**

```kotlin
import com.lifetracker.mobile.core.theme.ThemeController
import com.lifetracker.mobile.domain.model.ThemeMode
import org.koin.androidx.compose.get
```

```kotlin
val themeController: ThemeController = get()
val themeMode by themeController.themeMode.collectAsState()
LifeTrackerMobileTheme(themeMode = themeMode) { /* existing */ }
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/lifetracker/mobile/ui/theme/Color.kt \
       app/src/main/java/com/lifetracker/mobile/ui/theme/Theme.kt \
       app/src/main/java/com/lifetracker/mobile/MainActivity.kt
git commit -m "feat: make Compose theme adaptive"
```

---

### Task 9: Sanity checks (manual)

**Files:** none

- [ ] **Step 1: Run app**

Run from Android Studio or:
```
./gradlew :app:installDebug
```
Expected: App launches without crash.

- [ ] **Step 2: Toggle system dark/light**

Expected: UI updates live to dark/light.

- [ ] **Step 3: Restart app**

Expected: Theme matches system and no flash on launch.

---

## Notes
- Baseline tests couldn’t run due to missing Android SDK location (local.properties/ANDROID_HOME). Proceeding per user instruction.
- No automated tests added (per user request).
