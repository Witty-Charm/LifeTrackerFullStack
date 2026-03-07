# Research Document

---

## PART A — EXISTING CODEBASE STATE

### A1. UI State Model (`ui/model/UiState.kt`)

#### HeroScreenState
- `hero: HeroUi?` — null while loading or before first fetch
- `tasks: List<TaskUi>` — list of active tasks
- `isLoading: Boolean` — initial data load in progress
- `isActionLoading: Boolean` — task/hero action in progress
- `needsHeroCreation: Boolean` — no hero exists yet
- `criticalError: UiError?` — error blocking the home screen
- `actionError: UiError?` — error from a single action

#### HeroUi
| Field | Type | Notes |
|---|---|---|
| `id` | `Int` | |
| `name` | `String` | |
| `level` | `Int` | |
| `xpText` | `String` | e.g. "12/25" |
| `xpProgress` | `Float` | 0.0–1.0 |
| `hpText` | `String` | e.g. "29/50" |
| `hpProgress` | `Float` | 0.0–1.0 |
| `goldText` | `String` | formatted string |
| `isDead` | `Boolean` | |
| `isInRecovery` | `Boolean` | |
| `dailyText` | `String` | e.g. "1/5" |
| `dailyProgress` | `Float` | 0.0–1.0 |
| `statusBadge` | `HeroStatusBadge` | `Alive / Recovery / Dead` |

#### TaskUi
| Field | Type | Notes |
|---|---|---|
| `id` | `Int` | |
| `title` | `String` | |
| `description` | `String` | may be blank |
| `type` | `TaskType` | `Habit / OneTime` |
| `difficulty` | `TaskDifficulty` | `Easy / Medium / Hard / Epic` |
| `difficultyColor` | `Long` | ARGB color for badge |
| `isCompleted` | `Boolean` | |
| `isOverdue` | `Boolean` | |
| `dueDateText` | `String?` | null if no due date |
| `rewardText` | `String` | formatted XP+Gold reward |
| `penaltyText` | `String` | formatted XP+HP penalty |
| `streakText` | `String?` | null if no streak |

### A2. Current Theme (`ui/theme/`)

- `Theme.kt`: `LifeTrackerMobileTheme`. Light/dark toggled by `isSystemInDarkTheme()`.
- `Color.kt`: Only default Material3 scaffold colors: `Purple80`, `PurpleGrey80`, `Pink80`, `Purple40`, `PurpleGrey40`, `Pink40`. No custom palette defined.
- `DarkColorScheme`: `primary = Purple80 (#D0BCFF)`, `secondary = PurpleGrey80 (#CCC2DC)`, `tertiary = Pink80 (#EFB8C8)`.
- `LightColorScheme`: `primary = Purple40 (#6650a4)`, `secondary = PurpleGrey40 (#625b71)`, `tertiary = Pink40 (#7D5260)`.
- Typography: default Material3 `Typography` object, no font overrides.

### A3. Current Component Structure

#### HeroCard (`ui/components/HeroCard.kt`)
Layout (top-to-bottom inside a `Card`):
1. `Row`: hero name (`headlineMedium`, primary color) + status badge pill (colored `Surface`)
2. `Text`: "Level X" (`titleMedium`)
3. `Text` + `LinearProgressIndicator` (XP): bar color = `colorScheme.primary`, track = `surfaceVariant`
4. `Text` + `LinearProgressIndicator` (HP): bar color = `#E53935`, track = `surfaceVariant`
5. `Text` + `LinearProgressIndicator` (Daily): bar color = `#1E88E5`, track = `surfaceVariant`
6. `Text`: goldText (`titleMedium`, `#B8860B`)
7. Conditional: `Button("Respawn")` if `isDead`, else `Button("Heal")`

Progress bars: `height = 12.dp`, `clip(CircleShape)`, `StrokeCap.Round`.  
`onRespawn: () -> Unit`, `onHeal: () -> Unit` parameters present.

#### TaskItem (`ui/components/TaskItem.kt`)
Layout (top-to-bottom inside a `Card`):
1. `Row`: task title (`titleMedium`, weight 1f) + difficulty badge (`Surface`, `difficultyColor`, white text, `labelMedium`)
2. Optional description text (`bodyMedium`, `onSurfaceVariant`, max 3 lines)
3. `Text`: rewardText (`bodyMedium`, `onSurfaceVariant`)
4. `Text`: penaltyText (`bodyMedium`, `onSurfaceVariant`)
5. Optional streakText (`bodyMedium`, `onSurfaceVariant`)
6. Optional dueDateText (`bodyMedium`, red if `isOverdue`, else `LocalContentColor`)
7. `Row` end-aligned: `OutlinedButton("Fail")` + `Button("Complete")` + `Button("Delete")`

Parameters: `task`, `onCompleteClick`, `onFailClick`, `isActionLoading`, `onDeleteClick`.  
Enabled guard: `!task.isCompleted && !isActionLoading`.

### A4. Navigation (`navigation/NavGraph.kt`)

Routes in `Screen` sealed class:
- `object Home : Screen("home")` — start destination
- `object AddTask : Screen("add_task")` — legacy, to be replaced

Current `NavGraph` parameters reference deleted `Hero` and `GameTask` data types (broken).

### A5. Files in `ui/screens/`
- `HomeScreen.kt`: `HeroCard` + `LazyColumn` of `TaskItem` + FAB (navigate to create task)
- `AddTaskScreen.kt`: form with Title, Description, XP Reward fields + Save/Cancel (legacy, to be deleted)
- A third screen file exists (structure only, per directory listing)

---

## PART B — REFERENCE DESIGN ANALYSIS

Reference: provided screenshot (Habitica-style RPG habit tracker, dark theme)

### B1. COLOR PALETTE

#### Background colors
| Role | Value (approximated) |
|---|---|
| App background | `#12121A` — near-black with a blue-violet cast |
| Screen background | `#1A1A2E` — very dark navy/indigo |
| System bar / status bar | Same as app background (`#12121A`) |

#### Card / Surface colors
| Role | Value (approximated) |
|---|---|
| Task card background | `#252535` — dark charcoal-purple |
| Task card left accent strip (yellow) | `#F5C842` — saturated gold-yellow |
| Task card right action zone background (first item) | `#F5C842` (matching left strip, "complete" state visual) |
| Task card right action zone background (remaining items) | `#2E2E3E` — slightly lighter than card body |
| Quest/objective card background | `#1E1E2E` — slightly lighter than screen background |
| Quest/objective card border | ~`#7B2FBE` — medium purple, 1–2dp stroke |

#### Accent colors
| Role | Value (approximated) |
|---|---|
| Primary accent (FAB, bottom nav active, objective progress bar) | `#7C3AED` — vivid violet-purple |
| FAB background | `#7C3AED` |
| Bottom nav active icon + label | `#7C3AED` |
| Task left accent / complete button | `#F5C842` — gold-yellow |
| Coin / gold icon | `#F5C842` |
| Diamond / gem icon | `#5B8DEF` — medium blue-violet |
| XP (star icon) | `#F5C842` (same gold) |
| Objective reward badge background | `#F5C842` |
| Objective reward badge text | `#12121A` (dark, for contrast) |

#### Text colors
| Role | Value (approximated) |
|---|---|
| Primary text (titles, hero name) | `#FFFFFF` — pure white |
| Secondary text (subtitles, task description, stat labels) | `#A0A0B8` — muted lavender-grey |
| "Health" / "Experience" stat labels | `#A0A0B8` |
| Level text ("Lvl 1") | `#A0A0B8` |
| Coin/gem values | `#FFFFFF` |
| Bottom nav inactive label | `#A0A0B8` |

#### Progress bar colors
| Role | Value (approximated) |
|---|---|
| Health bar fill | `#E53935` — red (same as current HeroCard.kt) |
| Health bar track | `#2E2E3E` — dark charcoal |
| XP bar fill | `#F5C842` — gold-yellow |
| XP bar track | `#2E2E3E` — dark charcoal |
| Objective progress bar fill | `#7C3AED` — purple (matches FAB accent) |
| Objective progress bar track | `#2E2E3E` |

---

### B2. TYPOGRAPHY

#### Font characteristics
- Font family: appears to be a **geometric sans-serif** (similar to Nunito, Poppins, or Outfit) — rounded letterforms, no serifs
- Characters have notably rounded terminals

#### Weight usage
| Usage | Weight |
|---|---|
| Hero name ("Chukwuebuka") | Bold (700) |
| Objective title ("Starting Objectives") | Bold (700) |
| Task title ("Add a task to Habit") | SemiBold (600) |
| Task description (secondary line) | Regular (400) |
| Stat labels ("Health", "Experience") | Regular (400) |
| "Lvl 1", coin/gem values | Regular–Medium (400–500) |
| Bottom nav labels | Regular (400) |

#### Size hierarchy (relative, screen ~412dp wide)
| Level | Approximate sp | Usage |
|---|---|---|
| H1 | ~20sp | Hero name |
| H2 | ~16sp | Objective card title, task title |
| Body1 | ~13sp | Task description, stat labels |
| Label | ~11sp | "Lvl 1", bottom nav labels, coin text |
| Caption | ~10sp | Stat numbers ("29/50", "12/25") |

---

### B3. COMPONENT ANALYSIS

#### B3.1 Hero section (top area — no named "HeroCard" component visible)

The hero section is **not** a separate card with a border/elevation. It sits directly on the screen background.

Layout (left-to-right, two columns):
- **Left column**: square artwork tile (~130×130dp), purple gradient background, diamond icon in center. Below it: "Lvl 1" label + coin value + gem value on one row.
- **Right column**: two stat rows stacked vertically
  - Row 1: ❤️ icon + `LinearProgressIndicator` (red fill) + "29/50" fraction text + "Health" label
  - Row 2: ⭐ icon + `LinearProgressIndicator` (gold fill) + "12/25" fraction text + "Experience" label

No visible card border, no elevation, no heal/respawn button directly on this surface.  
The stat numbers ("29/50", "12/25") are **inside** the progress bar track, overlaid as centered text.  
The labels ("Health", "Experience") are to the **right** of the progress bar, outside it.

#### B3.2 Objective / Quest Card

One card below the hero section with:
- Dark background (`#1E1E2E`) + purple border stroke
- Left side: objective title in bold white
- Right side: coin reward badge (yellow pill, dark text, coin icon + number)
- Below title: one `LinearProgressIndicator` (purple fill, dark track) spanning full width
- Below bar: progress fraction text ("1/5") right-aligned, muted color

Corresponds to `dailyText` + `dailyProgress` from `HeroUi`.

#### B3.3 Task Items

Each task item is a horizontal card with three zones:

| Zone | Width | Content |
|---|---|---|
| Left strip | ~44dp | Circular `+` button on yellow background, fully rounded left corners |
| Body | flex (weight 1) | Title text (semibold white) + description text (muted grey), dark background |
| Right strip | ~60dp | Circular `−` button, dark or yellow background depending on item state |

- Corners: left strip has fully rounded left edge, right strip has fully rounded right edge. The body zone has square/flat joins with both strips.
- No difficulty badge visible on task items in this design.
- No reward/penalty text visible on task items in this design.
- No streak or due date text visible on task items in this design.
- The `+` action = complete/add. The `−` action = fail/remove.
- Card height appears uniform (~68–72dp).

#### B3.4 Bottom Navigation

Fixed `BottomNavigationBar` with 4 destinations:
| Position | Icon type | Label |
|---|---|---|
| 1 | Custom grid/bar icon | Habits |
| 2 | Calendar icon | Dailies |
| 3 | Checkmark-circle icon | To Do's |
| 4 | Trophy/award icon | Rewards |

- Active item: icon + label both in `#7C3AED` (purple)
- Inactive items: icon + label both in `#A0A0B8` (muted grey)
- Bar background: `#1A1A2E` (same as screen background, no visible separation)
- No elevation shadow visible; blends with screen background

#### B3.5 FAB

- Shape: circle (~56dp)
- Background: `#7C3AED` (purple, matches accent)
- Icon: `+` in white
- Position: horizontally centered, sitting at top edge of bottom navigation bar (overlapping the nav bar)
- Elevation: visible drop shadow

---

### B4. WHAT TO ADAPT FOR LIFETRACKER

#### Maps directly to our data model

| Reference element | LifeTracker field |
|---|---|
| Hero name ("Chukwuebuka") | `HeroUi.name` |
| "Lvl 1" label | `HeroUi.level` |
| Coin value (🪙 2.02) | `HeroUi.goldText` |
| Health bar (`29/50`) | `HeroUi.hpProgress` + `HeroUi.hpText` |
| XP bar (`12/25`) | `HeroUi.xpProgress` + `HeroUi.xpText` |
| Objective card progress bar | `HeroUi.dailyProgress` + `HeroUi.dailyText` |
| Task title | `TaskUi.title` |
| Task description (secondary line) | `TaskUi.description` |
| Left `+` button on task | `TaskItem.onCompleteClick` |
| Right `−` button on task | `TaskItem.onFailClick` |
| Daily/objective reward badge | can map to a reward value from domain (not in `HeroUi`, derived) |

#### Needs modification

| Current element | Required change |
|---|---|
| `HeroCard` is a separate elevated `Card` | Hero section becomes background-flush layout (no card border/elevation) |
| HP label + bar as two stacked rows inside card | HP bar with stat label right-aligned, fraction overlaid in bar; place in right column next to artwork tile |
| XP label + bar as two stacked rows inside card | Same treatment as HP: icon + bar + fraction + label |
| Gold displayed as plain `Text` | Gold shown as icon (`🪙`) + formatted value |
| No artwork tile | Add a colored square container (purple gradient tile) as avatar placeholder |
| `dailyText` + `dailyProgress` are inside HeroCard column | Move to a separate "Objective Card" below the hero section, with purple border |
| `statusBadge` pill (Alive/Recovery/Dead) | No equivalent pill in reference; status may be surfaced via color state of HP bar or a separate indicator — decision needed |
| `TaskItem` shows difficulty badge | No difficulty badge in reference design; can be removed or demoted to optional metadata |
| `TaskItem` shows rewardText + penaltyText | Not present in reference task rows; can be moved to a detail/expand state or removed from list view |
| `TaskItem` shows streakText + dueDateText | Not shown in reference; same decision needed |
| `TaskItem` has 3 buttons: Complete, Fail, Delete | Reference: 2 zones (left `+` strip, right `−` strip). Delete needs a separate mechanism (e.g., swipe-to-delete or long-press menu) |
| Bottom nav: none (only FAB currently wired) | Add 4-item `BottomNavigationBar` |
| FAB: anchored to scaffold bottom-right corner | FAB: anchored center, overlapping bottom nav |

#### Skip / not applicable

| Reference element | Reason |
|---|---|
| Gem/diamond currency icon (blue ◆) | LifeTracker domain has no gem/diamond currency; only gold |
| "Rewards" nav tab | No rewards screen in current nav graph; out of scope unless added |
| Hero avatar artwork (diamond icon) | Placeholder tile sufficient; no avatar upload in domain model |
| "Dailies" and "Habits" as separate tabs | LifeTracker uses a single task list; `TaskType` (`Habit / OneTime`) maps to these categories but as a filter, not separate nav destinations |

---

*End of research document.*
