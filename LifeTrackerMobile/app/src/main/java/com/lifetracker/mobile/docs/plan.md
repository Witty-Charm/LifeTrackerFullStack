# Implementation Plan — LifeTracker Mobile UI Redesign

**Source:** `docs/design.md`  
**State model:** `HeroScreenState` / `HeroUi` / `TaskUi` — no changes  
**ViewModel:** `HeroViewModel` — no changes  

---

## Phase 1 — Theme

> **Goal:** App compiles and launches with the dark RPG color scheme and Nunito typography. No layout changes yet.

---

### 1.1 `app/build.gradle` — MODIFY

**Add dependency:**
```groovy
implementation 'androidx.compose.ui:ui-text-google-fonts'
```

The BOM (`compose-bom:2026.02.00`) already manages the version — no explicit version needed.

**No other gradle changes.** `material-icons-extended` is already present.

---

### 1.2 `ui/theme/Color.kt` — REPLACE

Remove all existing constants (`Purple80`, `PurpleGrey80`, `Pink80`, `Purple40`, `PurpleGrey40`, `Pink40`).

Define the following 14 constants (all `val`, type `Color`):

| Constant | Hex |
|---|---|
| `AppBackground` | `0xFF12121A` |
| `SurfaceDark` | `0xFF1A1A2E` |
| `CardBackground` | `0xFF252535` |
| `CardBorder` | `0xFF2E2E3E` |
| `ObjectiveCardBg` | `0xFF1E1E2E` |
| `PurpleAccent` | `0xFF7C3AED` |
| `PurpleBorder` | `0xFF7B2FBE` |
| `HeroTileGradientStart` | `0xFF4A1D96` |
| `HeroTileGradientEnd` | `0xFF2E1065` |
| `GoldYellow` | `0xFFF5C842` |
| `HealthRed` | `0xFFE53935` |
| `TextPrimary` | `0xFFFFFFFF` |
| `TextSecondary` | `0xFFA0A0B8` |
| `OnGoldText` | `0xFF12121A` |

---

### 1.3 `ui/theme/Theme.kt` — REPLACE

- Remove `isSystemInDarkTheme()` import and toggle.
- Remove `LightColorScheme`.
- Define one `DarkColorScheme` using `darkColorScheme(...)`:

| Role | Constant |
|---|---|
| `background` | `AppBackground` |
| `surface` | `CardBackground` |
| `onBackground` | `TextPrimary` |
| `onSurface` | `TextPrimary` |
| `onSurfaceVariant` | `TextSecondary` |
| `primary` | `PurpleAccent` |
| `onPrimary` | `TextPrimary` |
| `secondary` | `GoldYellow` |
| `onSecondary` | `OnGoldText` |
| `error` | `HealthRed` |
| `outline` | `PurpleBorder` |
| `surfaceVariant` | `CardBorder` |

- Replace `SideEffect` body: `window.statusBarColor = Color.TRANSPARENT` (or `AppBackground.toArgb()`); `isAppearanceLightStatusBars = false`.
- `LifeTrackerMobileTheme` signature: `fun LifeTrackerMobileTheme(content: @Composable () -> Unit)` — remove `darkTheme` parameter entirely.
- All call sites of `LifeTrackerMobileTheme(...)` in `MainActivity.kt` must drop the `darkTheme` argument (check for any explicit passing).

---

### 1.4 `ui/theme/Type.kt` — REPLACE

**Font setup:**
```
val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage  = "com.google.android.gms",
    certificates     = R.array.com_google_android_gms_fonts_certs
)
val NunitoFont = GoogleFont("Nunito")
val NunitoFontFamily = FontFamily(
    Font(googleFont = NunitoFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = NunitoFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = NunitoFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = NunitoFont, fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = NunitoFont, fontProvider = provider, weight = FontWeight.ExtraBold)
)
```

**`Typography` object — define exactly these styles, all using `NunitoFontFamily`:**

| Style slot | Weight | Size | Usage |
|---|---|---|---|
| `displaySmall` | Bold | 20sp | Hero name |
| `headlineSmall` | Bold | 17sp | Objective card title |
| `titleMedium` | SemiBold | 15sp | Task title |
| `titleSmall` | SemiBold | 13sp | Stat labels |
| `bodyMedium` | Regular | 13sp | Descriptions, secondary text |
| `bodySmall` | Regular | 11sp | "Lvl X", nav labels, sub-info |
| `labelMedium` | Bold | 11sp | Badge text, coin values |

All other `Typography` slots left as Material3 defaults (do not override).

**Required import:** `androidx.compose.ui.text.googlefonts.*`  
**Required resource:** `res/values/font_certs.xml` — add `<array name="com_google_android_gms_fonts_certs">` with the GMS cert string (one-time boilerplate; copy from official Google Fonts for Compose docs).

---

### Phase 1 — Build Checkpoint

- `./gradlew :app:assembleDebug` succeeds with zero errors.
- App launches on emulator/device with a **fully black/dark-navy background** — no white or grey screen flash.
- No white toolbar or status bar.
- Font rendering: Nunito loads asynchronously from Google Fonts; initial render may fall back to system sans-serif — acceptable.

---

## Phase 2 — Components

> **Goal:** All four new/modified UI components compile and are individually previewable. `HomeScreen` is not yet wired to these components in this phase.

---

### 2.1 `ui/components/HeroSection.kt` — NEW

**Replaces:** `HeroCard.kt` (do **not** delete yet — `HomeScreen` still references it)

**Parameters:**
```kotlin
fun HeroSection(
    hero: HeroUi,
    onHeal: () -> Unit,
    onRespawn: () -> Unit,
    modifier: Modifier = Modifier
)
```

**Layout:** See `design.md §2` for full structure. Key implementation notes:

- Avatar tile: `Box` with `Modifier.size(130.dp).clip(RoundedCornerShape(16.dp)).background(Brush.verticalGradient(listOf(HeroTileGradientStart, HeroTileGradientEnd)))`. Center `Text` of `hero.name.first().uppercaseChar().toString()` at `fontSize = 48.sp, fontWeight = ExtraBold`.
- Level/gold row: `Row` with `Text("Lvl ${hero.level}")` + `Icon` (coin symbol: use `Icons.Default.MonetizationOn` or `"🪙"` as `Text`) + `Text(hero.goldText)`.
- `StatBar` private composable: `Box` overlaying `LinearProgressIndicator` + centered `Text(fractionText)`. Full spec in `design.md §2 / StatBar`.
- Action row: show `Button("Respawn")` only if `hero.isDead`; show `Button("Heal")` only if `hero.isInRecovery && !hero.isDead`. Hide entirely if both false.
- No `Card` wrapper, no elevation. Root composable is a plain `Column`.

**Fields consumed:** `hero.name`, `hero.level`, `hero.goldText`, `hero.hpProgress`, `hero.hpText`, `hero.xpProgress`, `hero.xpText`, `hero.isDead`, `hero.isInRecovery`  
**Fields not used in this component:** `hero.dailyText`, `hero.dailyProgress`, `hero.statusBadge` (moved to `DailyObjectiveCard` and action-button logic respectively)

---

### 2.2 `ui/components/DailyObjectiveCard.kt` — NEW

**Parameters:**
```kotlin
fun DailyObjectiveCard(
    hero: HeroUi,
    modifier: Modifier = Modifier
)
```

**Layout:** See `design.md §3`. Key notes:

- Outer `Box`: `Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.5.dp, PurpleBorder, RoundedCornerShape(12.dp)).background(ObjectiveCardBg).padding(16.dp)`.
- Title row: `Text("Daily Progress", style = MaterialTheme.typography.headlineSmall)` + gold pill `Surface(color = GoldYellow, shape = RoundedCornerShape(999.dp))` containing `Text(hero.dailyText, color = OnGoldText)`.
- Progress bar: `LinearProgressIndicator(progress = { hero.dailyProgress }, color = PurpleAccent, trackColor = CardBorder)`, `Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)`.

**Fields consumed:** `hero.dailyText`, `hero.dailyProgress`

---

### 2.3 `ui/components/TaskItem.kt` — REPLACE BODY

**Signature stays identical** to current file. No parameter changes.

**Key implementation notes:**

- Wrap content in `SwipeToDismissBox` (import: `androidx.compose.material3.SwipeToDismissBox`, `SwipeToDismissBoxValue`, `rememberSwipeToDismissBoxState`).
  - `confirmValueChange`: return `true` only for `EndToStart`; call `onDeleteClick()` there.
  - `backgroundContent`: red `Box` with `Icons.Default.Delete` aligned to `CenterEnd`.
- Main card: `Row` with `Modifier.heightIn(min = 68.dp).clip(RoundedCornerShape(12.dp)).graphicsLayer { alpha = cardAlpha }`.
- Left strip shape: `RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp, topEnd = 0.dp, bottomEnd = 0.dp)`.
- Right strip shape: `RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 12.dp, bottomEnd = 12.dp)`.
- `+` label on left strip: use `Text("+")` — do **not** use `Icon`.
- `−` label on right strip: use `Text("−")` — do **not** use `Icon`.
- Difficulty shown as `Text(task.difficulty.name, style = bodySmall, color = Color(task.difficultyColor))` inside body column — **not** a `Surface` badge.
- `rewardText` and `penaltyText`: **not rendered** in this view.
- `streakText`: render if not null as `Text("🔥 ${task.streakText}")`.
- `dueDateText`: render if not null; color = `HealthRed` if `isOverdue`, else `TextSecondary`.
- `cardAlpha = if (task.isCompleted) 0.5f else 1.0f` — applied via `graphicsLayer`.
- `canAct = !task.isCompleted && !isActionLoading` — both button click handlers and `SwipeToDismissBox` `enabled` state use this guard.

---

### 2.4 `ui/components/GameBottomNavigationBar.kt` — NEW

**`HomeTab` enum — define in this file (or a shared `ui/model/HomeTab.kt`):**
```kotlin
enum class HomeTab(val label: String) {
    Habits("Habits"),
    Dailies("Dailies"),
    ToDos("To Do's"),
    Rewards("Rewards")
}
```

**Parameters:**
```kotlin
fun GameBottomNavigationBar(
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit
)
```

**Layout:**
- `NavigationBar(containerColor = SurfaceDark, tonalElevation = 0.dp)`
- One `NavigationBarItem` per `HomeTab.entries` in order.
- Icon mapping:

| Tab | Icon |
|---|---|
| `Habits` | `Icons.Default.FitnessCenter` |
| `Dailies` | `Icons.Default.CalendarToday` |
| `ToDos` | `Icons.Default.CheckCircleOutline` |
| `Rewards` | `Icons.Default.EmojiEvents` |

- `NavigationBarItemDefaults.colors(selectedIconColor = PurpleAccent, selectedTextColor = PurpleAccent, unselectedIconColor = TextSecondary, unselectedTextColor = TextSecondary, indicatorColor = Color.Transparent)`.

---

### Phase 2 — Build Checkpoint

- `./gradlew :app:assembleDebug` succeeds with zero errors.
- `@Preview` annotations on all four new/modified components render correctly in Android Studio — verify visually:
  - `HeroSection`: two-column layout, gradient tile, red HP bar, gold XP bar, stat fraction overlaid in bar.
  - `DailyObjectiveCard`: purple-bordered card, progress bar.
  - `TaskItem`: three-zone row, yellow left strip, dark right strip, swipe background visible in preview if using `SwipeToDismissBox`.
  - `GameBottomNavigationBar`: 4 tabs, purple active item.
- `HomeScreen` still compiles and runs using old `HeroCard` — no regression.

---

## Phase 3 — HomeScreen

> **Goal:** `HomeScreen` is fully rewired to all new components. `HeroCard.kt` is deleted. App is end-to-end functional with the new UI.

---

### 3.1 `ui/screens/HomeScreen.kt` — MODIFY

**Remove:**
- Import of `HeroCard`, any reference to it.
- Old `Scaffold` slot configuration.
- Old `FloatingActionButton` placement.

**Add:**
- Import `HeroSection`, `DailyObjectiveCard`, `GameBottomNavigationBar`, `HomeTab`.
- Local state: `var selectedTab by remember { mutableStateOf(HomeTab.ToDos) }`.
- New `Scaffold`:
  ```
  Scaffold(
      containerColor = AppBackground,
      bottomBar = {
          GameBottomNavigationBar(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
      },
      floatingActionButton = {
          FloatingActionButton(
              onClick = { navController.navigate(Screen.CreateTask.route) },
              shape = CircleShape,
              containerColor = PurpleAccent,
              contentColor = TextPrimary
          ) { Icon(Icons.Default.Add, contentDescription = "Add task", modifier = Modifier.size(28.dp)) }
      },
      floatingActionButtonPosition = FabPosition.Center
  )
  ```
- Content column (inside scaffold padding):
  - `if (state.isLoading)` → centered `CircularProgressIndicator(color = PurpleAccent)`.
  - `else if (state.criticalError != null)` → `ErrorView(onRetry = { vm.loadData() })`.
  - `else` → `HeroSection` + `DailyObjectiveCard` + filtered `LazyColumn`.
- Task filter logic:
  ```kotlin
  val filteredTasks = when (selectedTab) {
      HomeTab.Habits  -> state.tasks.filter { it.type == TaskType.Habit }
      HomeTab.ToDos   -> state.tasks.filter { it.type == TaskType.OneTime }
      else            -> emptyList()   // Dailies and Rewards are stub tabs
  }
  ```
- `LazyColumn`:
  - `contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)`
  - `verticalArrangement = Arrangement.spacedBy(8.dp)`
  - `items(filteredTasks, key = { it.id }) { task -> TaskItem(...) }`
- Empty state: `if (filteredTasks.isEmpty() && !state.isLoading)` → `Box(contentAlignment = Center) { Text("No tasks here yet", ...) }`.
- `HeroSection` guard: `state.hero?.let { hero -> HeroSection(...) }` — skip rendering if null.
- `DailyObjectiveCard` guard: same null check on `state.hero`.

**Private helpers to add in this file:**
- `ErrorView(onRetry: () -> Unit)`: column with error text + retry button (`PurpleAccent`).
- `EmptyTasksPlaceholder()`: centered `Text("No tasks here yet", color = TextSecondary)`.

---

### 3.2 `ui/components/HeroCard.kt` — DELETE

Delete after confirming:
1. `HomeScreen.kt` no longer imports or references `HeroCard`.
2. No other file imports `HeroCard`.

Search command to verify before deleting:  
`grep -r "HeroCard" app/src/main/`

---

### 3.3 `navigation/NavGraph.kt` — VERIFY (no changes expected)

Confirm `HomeScreen` composable invocation line still passes `vm`, `state`, `navController` — these parameters are unchanged. No action needed unless `HomeScreen` signature changes.

---

### Phase 3 — Build Checkpoint

- `./gradlew :app:assembleDebug` succeeds with zero errors.
- `HeroCard.kt` is absent from the source tree.
- App launches on emulator:
  - Full-black background visible immediately.
  - Hero section renders with gradient avatar tile, red HP bar, gold XP bar — no old card border.
  - Daily objective card below hero, purple-bordered.
  - Task list with yellow-strip task cards.
  - Bottom navigation with 4 tabs; active tab purple.
  - FAB centered, overlapping top of bottom nav.
  - Switching between Habits / To Do's tabs filters the task list.
  - Swiping a task left reveals red delete background, releasing triggers `vm.deleteTask()`.
  - Tapping `+` (left strip) calls `vm.completeTask()`.
  - Tapping `−` (right strip) calls `vm.failTask()`.
  - Completed tasks rendered at 50% alpha with both buttons unresponsive.

---

## File Change Summary

| Phase | Status | File |
|---|---|---|
| 1 | MODIFY | `app/build.gradle` |
| 1 | REPLACE | `ui/theme/Color.kt` |
| 1 | REPLACE | `ui/theme/Theme.kt` |
| 1 | REPLACE | `ui/theme/Type.kt` |
| 1 | ADD RESOURCE | `res/values/font_certs.xml` |
| 2 | NEW | `ui/components/HeroSection.kt` |
| 2 | NEW | `ui/components/DailyObjectiveCard.kt` |
| 2 | REPLACE BODY | `ui/components/TaskItem.kt` |
| 2 | NEW | `ui/components/GameBottomNavigationBar.kt` |
| 3 | MODIFY | `ui/screens/HomeScreen.kt` |
| 3 | DELETE | `ui/components/HeroCard.kt` |
| — | UNCHANGED | `ui/model/UiState.kt` |
| — | UNCHANGED | `ui/viewmodel/HeroViewModel.kt` |
| — | UNCHANGED | `navigation/NavGraph.kt` |
| — | UNCHANGED | `ui/screens/CreateHeroScreen.kt` |
| — | UNCHANGED | `ui/screens/CreateTaskScreen.kt` |
