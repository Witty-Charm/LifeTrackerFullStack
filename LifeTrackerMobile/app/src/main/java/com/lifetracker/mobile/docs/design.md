# Design Document — LifeTracker Mobile UI Redesign

**Theme:** Dark RPG  
**Based on:** `docs/research.md` Part B  
**Scope:** `ui/theme/`, `ui/components/`, `ui/screens/HomeScreen.kt`

---

## 1. THEME

### 1.1 `ui/theme/Color.kt` — REPLACE

Define a named constant for every semantic role. No unnamed magic hex literals anywhere else in the codebase.

| Constant name | Hex value | Role |
|---|---|---|
| `AppBackground` | `#12121A` | Root screen background |
| `SurfaceDark` | `#1A1A2E` | Screen-level surface / bottom nav |
| `CardBackground` | `#252535` | Task card body, hero section tile BG |
| `CardBorder` | `#2E2E3E` | Card track, divider, right strip |
| `ObjectiveCardBg` | `#1E1E2E` | Daily objective card background |
| `PurpleAccent` | `#7C3AED` | FAB, active nav, objective progress bar |
| `PurpleBorder` | `#7B2FBE` | Objective card border stroke |
| `HeroTileGradientStart` | `#4A1D96` | Avatar tile gradient top |
| `HeroTileGradientEnd` | `#2E1065` | Avatar tile gradient bottom |
| `GoldYellow` | `#F5C842` | XP bar fill, task left strip, coin icon, reward badge |
| `HealthRed` | `#E53935` | HP bar fill |
| `TextPrimary` | `#FFFFFF` | Titles, hero name, task title |
| `TextSecondary` | `#A0A0B8` | Labels, descriptions, inactive nav |
| `OnGoldText` | `#12121A` | Text on yellow/gold backgrounds |

Remove all previous Material3 scaffold defaults (`Purple80`, `PurpleGrey80`, etc.).

---

### 1.2 `ui/theme/Theme.kt` — REPLACE

```
Rules (no code, just spec):
- Remove isSystemInDarkTheme() toggle entirely.
- Only one ColorScheme defined: darkColorScheme.
- darkColorScheme mappings:
    background        = AppBackground
    surface           = CardBackground
    onBackground      = TextPrimary
    onSurface         = TextPrimary
    onSurfaceVariant  = TextSecondary
    primary           = PurpleAccent
    onPrimary         = TextPrimary
    secondary         = GoldYellow
    onSecondary       = OnGoldText
    error             = HealthRed
    outline           = PurpleBorder
    surfaceVariant    = CardBorder      (used as progress bar track by Material3)
- Remove SideEffect that writes statusBarColor.
- Set status bar color = AppBackground, isAppearanceLightStatusBars = false (always).
```

---

### 1.3 `ui/theme/Type.kt` — REPLACE

Font: **Nunito** (Google Fonts). Weights: ExtraBold (800), Bold (700), SemiBold (600), Regular (400).

| TextStyle name | Font weight | Size | Usage |
|---|---|---|---|
| `displaySmall` | Bold 700 | 20sp | Hero name |
| `headlineSmall` | Bold 700 | 17sp | Objective card title |
| `titleMedium` | SemiBold 600 | 15sp | Task item title |
| `titleSmall` | SemiBold 600 | 13sp | Stat labels ("Health", "Experience") |
| `bodyMedium` | Regular 400 | 13sp | Task description, secondary labels |
| `bodySmall` | Regular 400 | 11sp | "Lvl X", nav labels, sub-info |
| `labelMedium` | Bold 700 | 11sp | Reward badge text, coin values |

Implementation note: Add Nunito via `androidx.compose.ui:ui-text-google-fonts`. Define a `FontFamily` constant `NunitoFontFamily` with all four weight variants. Apply to `Typography` object using named style overrides above.

---

## 2. HERO SECTION

**File:** `ui/components/HeroSection.kt` — NEW (replaces `HeroCard.kt`)  
**Compose parent:** `Box` or `Column`, no `Card` container — flush on `AppBackground`

### Parameters
```
hero: HeroUi
onHeal: () -> Unit
onRespawn: () -> Unit
modifier: Modifier = Modifier
```

### Layout Structure

```
Column(modifier = fillMaxWidth + padding(horizontal=16dp, top=16dp)) {
  Row(horizontalArrangement=SpaceBetween, verticalAlignment=Top) {
    [LEFT COLUMN — weight(0.45f)]
    [RIGHT COLUMN — weight(0.55f)]
  }
  Spacer(8dp)
  [STATUS ROW — conditional]
}
```

#### Left Column
```
Column(horizontalAlignment=CenterHorizontally) {
  Box(
    modifier = size(130.dp)
              .clip(RoundedCornerShape(16.dp))
              .background(Brush.verticalGradient(HeroTileGradientStart, HeroTileGradientEnd))
  ) {
    Text(
      text = hero.name.first().uppercaseChar().toString(),
      style = typography.displaySmall.copy(fontSize=48.sp, fontWeight=ExtraBold),
      color = TextPrimary,
      modifier = align(Center)
    )
  }

  Spacer(8dp)

  Row(verticalAlignment=CenterVertically, horizontalArrangement=Center) {
    Text("Lvl ${hero.level}", style=bodySmall, color=TextSecondary)
    Spacer(12dp)
    Icon(coinIcon, tint=GoldYellow, size=14.dp)
    Text(hero.goldText, style=labelMedium, color=GoldYellow)
  }
}
```

Fields used: `hero.name`, `hero.level`, `hero.goldText`

#### Right Column
```
Column(verticalArrangement=spacedBy(12dp), modifier=fillMaxHeight+padding(start=12dp)) {
  StatBar(
    icon = ❤️ (drawable or emoji Text),
    progress = hero.hpProgress,
    fractionText = hero.hpText,
    label = "Health",
    barColor = HealthRed
  )
  StatBar(
    icon = ⭐ (drawable or emoji Text),
    progress = hero.xpProgress,
    fractionText = hero.xpText,
    label = "Experience",
    barColor = GoldYellow
  )
}
```

Fields used: `hero.hpProgress`, `hero.hpText`, `hero.xpProgress`, `hero.xpText`

#### StatBar (private helper composable)
```
Parameters: icon: String, progress: Float, fractionText: String, label: String, barColor: Color

Layout:
Row(verticalAlignment=CenterVertically) {
  Text(icon, fontSize=16.sp)                    // icon glyph
  Spacer(6dp)
  Box(modifier=weight(1f)) {
    LinearProgressIndicator(
      progress = { progress },
      modifier = fillMaxWidth + height(16.dp) + clip(CircleShape),
      color = barColor,
      trackColor = CardBorder
    )
    Text(
      text = fractionText,
      style = bodySmall,
      color = TextPrimary,
      modifier = align(Center)
    )
  }
  Spacer(6dp)
  Text(label, style=titleSmall, color=TextSecondary)
}
```

#### Status / Action Row (conditional — shown below both columns)
```
if (hero.isDead || hero.isInRecovery) {
  Row(modifier=fillMaxWidth+padding(top=8dp), horizontalArrangement=End) {
    if (hero.isDead) {
      Button(onClick=onRespawn, colors=ButtonDefaults.buttonColors(containerColor=HealthRed)) {
        Text("Respawn", color=TextPrimary)
      }
    } else if (hero.isInRecovery) {
      Button(onClick=onHeal, colors=ButtonDefaults.buttonColors(containerColor=PurpleAccent)) {
        Text("Heal", color=TextPrimary)
      }
    }
  }
}
```

Fields used: `hero.isDead`, `hero.isInRecovery`, `hero.statusBadge` (drives button display, no separate pill badge)

---

## 3. DAILY OBJECTIVE CARD

**File:** `ui/components/DailyObjectiveCard.kt` — NEW  
**Compose parent:** `Box` clipped to `RoundedCornerShape(12.dp)` with a `border` stroke

### Parameters
```
hero: HeroUi
modifier: Modifier = Modifier
```

### Layout Structure
```
Box(
  modifier = fillMaxWidth
           + padding(horizontal=16dp)
           + clip(RoundedCornerShape(12dp))
           + border(width=1.5.dp, color=PurpleBorder, shape=RoundedCornerShape(12dp))
           + background(ObjectiveCardBg)
           + padding(16dp)
) {
  Column {
    Row(horizontalArrangement=SpaceBetween, verticalAlignment=CenterVertically) {
      Text("Daily Progress", style=headlineSmall, color=TextPrimary, modifier=weight(1f))

      Surface(
        color = GoldYellow,
        shape = RoundedCornerShape(999.dp)
      ) {
        Row(modifier=padding(horizontal=10dp, vertical=4dp), verticalAlignment=CenterVertically) {
          Icon(coinIcon, tint=OnGoldText, size=12dp)
          Spacer(4dp)
          Text(hero.dailyText, style=labelMedium, color=OnGoldText)
        }
      }
    }

    Spacer(8dp)

    LinearProgressIndicator(
      progress = { hero.dailyProgress },
      modifier = fillMaxWidth + height(8dp) + clip(CircleShape),
      color = PurpleAccent,
      trackColor = CardBorder
    )
  }
}
```

Fields used: `hero.dailyText`, `hero.dailyProgress`

> **Note:** The gold pill badge on the objective card is a visual reward indicator from the reference. In LifeTracker, `dailyText` (e.g. "1/5") is used as the badge text since there is no separate daily reward field on `HeroUi`. If a daily coin reward value is added to `HeroUi` in the future, replace `dailyText` in the badge with that value.

---

## 4. TASK ITEM

**File:** `ui/components/TaskItem.kt` — REDESIGN (replace entire file body)

### Parameters (unchanged — keep existing signature)
```
task: TaskUi
onCompleteClick: () -> Unit
onFailClick: () -> Unit
isActionLoading: Boolean
onDeleteClick: () -> Unit
modifier: Modifier = Modifier
```

### State
```
val isCompleted = task.isCompleted
val canAct = !isCompleted && !isActionLoading
val cardAlpha = if (isCompleted) 0.5f else 1.0f
```

### Swipe-to-Dismiss (delete)
Wrap entire card in `SwipeToDismissBox`:
```
val dismissState = rememberSwipeToDismissBoxState(
  confirmValueChange = { value ->
    if (value == SwipeToDismissBoxValue.EndToStart) {
      onDeleteClick()
      true
    } else false
  }
)
SwipeToDismissBox(
  state = dismissState,
  backgroundContent = { DismissBackground() },      // red background with trash icon
  content = { TaskCardContent(...) }
)
```

`DismissBackground` composable:
```
Box(modifier=fillMaxSize+background(HealthRed)+padding(end=16dp)) {
  Icon(Icons.Default.Delete, tint=TextPrimary, modifier=align(CenterEnd))
}
```

### TaskCardContent — three-zone row

```
Row(
  modifier = fillMaxWidth
           + clip(RoundedCornerShape(12.dp))
           + graphicsLayer { alpha = cardAlpha }
) {
  [LEFT STRIP]
  [BODY]
  [RIGHT STRIP]
}
```

#### Left Strip
```
Box(
  modifier = width(48.dp)
           + fillMaxHeight()
           + background(GoldYellow, shape=RoundedCornerLeftOnly(12.dp))
           + clickable(enabled=canAct, onClick=onCompleteClick),
  contentAlignment = Center
) {
  Box(
    modifier = size(28.dp)
             + background(Color.White.copy(alpha=0.2f), shape=CircleShape),
    contentAlignment = Center
  ) {
    Text("+", style=titleMedium.copy(fontWeight=Bold), color=OnGoldText)
  }
}
```

Shape: `RoundedCornerShape(topStart=12.dp, bottomStart=12.dp, topEnd=0.dp, bottomEnd=0.dp)`

#### Body (flex)
```
Column(
  modifier = weight(1f)
           + background(CardBackground)
           + padding(horizontal=12.dp, vertical=10.dp)
) {
  Text(task.title, style=titleMedium, color=TextPrimary, maxLines=2, overflow=Ellipsis)

  if (task.description.isNotBlank()) {
    Spacer(2.dp)
    Text(task.description, style=bodyMedium, color=TextSecondary, maxLines=2, overflow=Ellipsis)
  }

  // Difficulty label — small text, no badge
  Spacer(4.dp)
  Text(
    text = task.difficulty.name,
    style = bodySmall,
    color = Color(task.difficultyColor).copy(alpha = 0.9f)
  )

  if (task.streakText != null) {
    Text("🔥 ${task.streakText}", style=bodySmall, color=TextSecondary)
  }

  if (task.dueDateText != null) {
    Text(
      text = task.dueDateText,
      style = bodySmall,
      color = if (task.isOverdue) HealthRed else TextSecondary
    )
  }
}
```

Fields used: `task.title`, `task.description`, `task.difficulty.name`, `task.difficultyColor`, `task.streakText`, `task.dueDateText`, `task.isOverdue`

Fields not shown in list view (design decision — omit from row): `task.rewardText`, `task.penaltyText`

#### Right Strip
```
Box(
  modifier = width(60.dp)
           + fillMaxHeight()
           + background(CardBorder, shape=RoundedCornerRightOnly(12.dp))
           + clickable(enabled=canAct, onClick=onFailClick),
  contentAlignment = Center
) {
  Box(
    modifier = size(28.dp)
             + border(1.5.dp, TextSecondary, CircleShape),
    contentAlignment = Center
  ) {
    Text("−", style=titleMedium, color=TextSecondary)
  }
}
```

Shape: `RoundedCornerShape(topStart=0.dp, bottomStart=0.dp, topEnd=12.dp, bottomEnd=12.dp)`

### Minimum card height
`modifier = heightIn(min=68.dp)` on the outer `Row`

---

## 5. HOME SCREEN

**File:** `ui/screens/HomeScreen.kt` — MODIFY existing file  
**Compose root:** `Scaffold`

### Scaffold slots

```
Scaffold(
  containerColor = AppBackground,
  bottomBar = { GameBottomNavigationBar(selectedTab, onTabSelected) },
  floatingActionButton = { GameFAB(onClick = { navController.navigate(Screen.CreateTask.route) }) },
  floatingActionButtonPosition = FabPosition.Center
) { innerPadding ->
  HomeScreenContent(state, vm, innerPadding, selectedTab)
}
```

### FAB composable (inline or extracted)
```
FloatingActionButton(
  onClick = onClick,
  shape = CircleShape,
  containerColor = PurpleAccent,
  contentColor = TextPrimary
) {
  Icon(Icons.Default.Add, contentDescription = "Add task", modifier=size(28.dp))
}
```

### Content area
```
Column(modifier = fillMaxSize + padding(innerPadding)) {
  when {
    state.isLoading -> CircularProgressIndicator(modifier=align(Center)+padding(top=64dp), color=PurpleAccent)
    state.criticalError != null -> ErrorView(state.criticalError, retry = { vm.loadData() })
    else -> {
      HeroSection(
        hero = state.hero ?: return,
        onHeal = { vm.healHero() },
        onRespawn = { vm.respawnHero() },
        modifier = padding(bottom=8.dp)
      )
      DailyObjectiveCard(
        hero = state.hero,
        modifier = padding(bottom=12.dp)
      )
      // Task list filtered by tab
      val filteredTasks = when (selectedTab) {
        Tab.Habits -> state.tasks.filter { it.type == TaskType.Habit }
        Tab.ToDos  -> state.tasks.filter { it.type == TaskType.OneTime }
        else       -> state.tasks
      }
      if (filteredTasks.isEmpty()) {
        EmptyTasksPlaceholder(modifier=weight(1f))
      } else {
        LazyColumn(
          modifier = weight(1f),
          contentPadding = PaddingValues(horizontal=16.dp, vertical=4.dp),
          verticalArrangement = spacedBy(8.dp)
        ) {
          items(filteredTasks, key = { it.id }) { task ->
            TaskItem(
              task = task,
              onCompleteClick = { vm.completeTask(task.id) },
              onFailClick = { vm.failTask(task.id) },
              onDeleteClick = { vm.deleteTask(task.id) },
              isActionLoading = state.isActionLoading
            )
          }
        }
      }
    }
  }
}
```

### Tab state
Local `HomeScreen` enum:
```
enum class HomeTab { Habits, Dailies, ToDos, Rewards }
var selectedTab by remember { mutableStateOf(HomeTab.ToDos) }
```

Default selected tab: `HomeTab.ToDos` (maps to `TaskType.OneTime`, the most common task type)

### ErrorView (private)
```
Column(modifier=fillMaxWidth+padding(32.dp), horizontalAlignment=CenterHorizontally) {
  Text("Something went wrong", style=titleMedium, color=TextSecondary)
  Spacer(12.dp)
  Button(onClick=retry, colors=ButtonDefaults.buttonColors(containerColor=PurpleAccent)) {
    Text("Retry")
  }
}
```

### EmptyTasksPlaceholder (private)
```
Box(modifier=fillMaxWidth+weight(1f), contentAlignment=Center) {
  Text("No tasks here yet", style=bodyMedium, color=TextSecondary)
}
```

---

## 6. BOTTOM NAVIGATION

**File:** `ui/components/GameBottomNavigationBar.kt` — NEW

### Parameters
```
selectedTab: HomeTab
onTabSelected: (HomeTab) -> Unit
```

### Tab definitions
| Tab | Icon (Material Icons) | Label | Filter behavior |
|---|---|---|---|
| `Habits` | `Icons.Default.FilterList` | "Habits" | `task.type == TaskType.Habit` |
| `Dailies` | `Icons.Default.CalendarToday` | "Dailies" | Stub — shows empty `LazyColumn` |
| `ToDos` | `Icons.Default.CheckCircleOutline` | "To Do's" | `task.type == TaskType.OneTime` |
| `Rewards` | `Icons.Default.EmojiEvents` | "Rewards" | Stub — shows empty `LazyColumn` |

### Composable structure
```
NavigationBar(
  containerColor = SurfaceDark,
  tonalElevation = 0.dp
) {
  HomeTab.entries.forEach { tab ->
    NavigationBarItem(
      selected = selectedTab == tab,
      onClick = { onTabSelected(tab) },
      icon = { Icon(tab.icon, contentDescription = tab.label) },
      label = { Text(tab.label, style = bodySmall) },
      colors = NavigationBarItemDefaults.colors(
        selectedIconColor = PurpleAccent,
        selectedTextColor = PurpleAccent,
        unselectedIconColor = TextSecondary,
        unselectedTextColor = TextSecondary,
        indicatorColor = Color.Transparent   // no pill highlight behind active icon
      )
    )
  }
}
```

Elevation: `tonalElevation = 0.dp` — nav bar blends with `SurfaceDark`, no visible divider line.

---

## 7. FILE CHANGE SUMMARY

| Status | File | Note |
|---|---|---|
| REPLACE | `ui/theme/Color.kt` | Full dark palette |
| REPLACE | `ui/theme/Theme.kt` | Always dark, new color scheme |
| REPLACE | `ui/theme/Type.kt` | Nunito font, 7-level hierarchy |
| NEW | `ui/components/HeroSection.kt` | Replaces `HeroCard.kt` functionally |
| KEEP | `ui/components/HeroCard.kt` | Delete after `HeroSection` is wired |
| NEW | `ui/components/DailyObjectiveCard.kt` | Extracted from what was inside `HeroCard` |
| REPLACE body | `ui/components/TaskItem.kt` | New 3-zone layout, same signature |
| NEW | `ui/components/GameBottomNavigationBar.kt` | 4-tab nav bar |
| MODIFY | `ui/screens/HomeScreen.kt` | New layout with all above components |

---

## 8. DESIGN DECISIONS NOT IN REFERENCE

| Decision | Rationale |
|---|---|
| `statusBadge` surfaced via button visibility only | Reference has no explicit badge; showing Heal/Respawn button only when relevant communicates status implicitly |
| `rewardText` / `penaltyText` omitted from list row | Reference does not show these; reduces visual noise; available in `TaskUi` for a future detail sheet |
| `difficultyColor` used as text color, not badge | Preserves difficulty information without the badge block from old design |
| Filter chips as bottom nav tabs | Avoids adding new screens; `Dailies` and `Rewards` are stubs to satisfy the reference nav structure |
| Default tab = `ToDos` | `TaskType.OneTime` is the general-purpose task type |
| Swipe-to-delete direction = right-to-left (EndToStart) | Standard Android gesture convention |

---

*End of design document.*
