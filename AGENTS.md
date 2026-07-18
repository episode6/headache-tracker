# AGENTS.md — Headache Tracker

Guidance for AI assistants and contributors working in this codebase. Read this before making structural changes.

## Product summary

Headache Tracker stores one entry per calendar day (`YYYY-MM-DD` primary key) with:

- `intensity`: 0–3 (none → severe)
- `pillsTaken`: 0–2

The calendar visualizes intensity with fixed theme colors (`Intensity0`–`Intensity3` in `ui/theme/Color.kt`). Pills are shown as small dots on day cells.

There is no backend. Export/import uses versioned JSON files via `HeadacheBackupManager`.

**No network access — ever.** This app must never request the `INTERNET` permission; being fully offline is part of the product spec. Anything update- or web-related must go through the browser instead: the "Check for updates" menu item just opens the GitHub commits page (snapshot builds) or latest-release page (release builds) via an `ACTION_VIEW` URL, chosen at build time through the `check_for_updates_url` resValue in `app/build.gradle.kts`. Enforced by the `release-verification` convention plugin in `build-logic/` (an included build): `:app:verifyReleasePermissions` (merged-manifest permissions pinned to `app/expected-permissions.txt`) and `:app:verifyReleaseDependencies` (release dependency set pinned to `app/expected-dependencies.txt`), both wired into `check` and therefore CI. Convention plugins must stay in the `build-logic` included build, **never buildSrc**: buildSrc's parent classloader isolates AGP from the Kotlin compiler plugins, which silently disables Metro codegen and the app then crashes on launch.

---

## Package map

| Package | Responsibility |
|---------|----------------|
| `data/` | Room (`HeadacheDatabase`, `HeadacheDao`), migrations, backup I/O |
| `di/` | Metro `AppGraph`, `AppViewModelFactory` |
| `model/` | `@Entity` types, kotlinx-serialization backup DTOs, mapping helpers |
| `ui/<feature>/` | One feature folder per screen: Composable(s) + ViewModel |
| `ui/navigation/` | NavHost, adaptive scaffold, route types, composition root wiring |
| `ui/theme/` | `HeadacheTrackerTheme`, M3 color scheme, typography, intensity colors |

Root types:

- `HeadacheTrackerApp` — creates `AppGraph` on startup
- `Context.appGraph` — extension to reach the graph from Composables / Activity
- `MainActivity` — edge-to-edge + `HeadacheTrackerTheme` + `HeadacheTrackerNavigation()`

---

## Architectural patterns

### MVVM + unidirectional data flow

Every screen follows the same contract:

```kotlin
@Composable
fun SomeScreen(
    state: SomeState,           // immutable data class
    onSomething: () -> Unit,   // event callbacks
    ...
)
```

- ViewModels expose `StateFlow<SomeState>` (or `SharedFlow` for one-shot events).
- Composables **do not** call DAOs, launch coroutines for business logic, or hold mutable domain state.
- Collect state in the navigation/wiring layer: `val state by viewModel.state.collectAsState()`.

`CalendarViewModel` combines multiple sources with `combine { ... }.stateIn(...)`. `EditViewModel` uses a single `MutableStateFlow`.

### One-shot UI events

Use `SharedFlow` for ephemeral messages (snackbars, toasts), not `StateFlow`:

```kotlin
// ViewModel
private val _dataTransferMessages = MutableSharedFlow<DataTransferMessage>()
val dataTransferMessages = _dataTransferMessages.asSharedFlow()

// Navigation layer
LaunchedEffect(viewModel) {
    viewModel.dataTransferMessages.collectLatest { ... snackbarHostState.showSnackbar(...) }
}
```

### Dependency injection (Metro)

- **`AppGraph`** (`@DependencyGraph`, `@SingleIn(AppScope::class)`) provides app-scoped singletons: database, DAO, `AppViewModelFactory`.
- ViewModels use `@Inject` (or `@AssistedInject` when they need runtime parameters).
- **`AppViewModelFactory`** maps ViewModel classes to providers. Parameterized ViewModels pass data via `CreationExtras`:

```kotlin
viewModel(
    key = dateKey,
    factory = viewModelFactory,
    extras = MutableCreationExtras().apply {
        set(AppViewModelFactory.EditDateKey, dateKey)
    },
)
```

When adding a new ViewModel:

1. Annotate with `@Inject` (or `@AssistedInject` + `@AssistedFactory`).
2. Register in `AppViewModelFactory.create()`.
3. If Metro needs explicit binding, follow existing `@ContributesBinding` usage on the factory.

Do **not** introduce Hilt/Dagger unless the project explicitly migrates.

### Navigation model

**Type-safe routes** live in `ui/navigation/Routes.kt`:

```kotlin
@Serializable sealed interface Route {
    @Serializable data object Calendar : Route
    @Serializable data class EditEntry(val date: String) : Route  // defined but unused in NavHost
}
```

#### Screen flow

1. **Calendar (Initial)**: The app launches to the calendar.
   - **Requirement**: The calendar screen **must take up the full screen by default**, even on large or unfolded devices (e.g. Pixel 10 Pro Fold). We force a single-pane layout (`maxHorizontalPartitions = 1`) whenever `selectedDate` is null.
2. **Edit Entry**: Tapping a day selects a date.
   - **Small screens (Stacked)**: The edit pane slides in, covering the calendar.
   - **Large screens (Side-by-side)**: The edit pane appears next to the calendar.
3. **Dismissal**:
   - **Stacked**: Saving or Back returns to the full-screen calendar.
   - **Side-by-side**: Saving shows a Toast; the pane stays open for further edits until the user navigates back.

Current navigation is **hybrid**:

| Concern | Mechanism |
|---------|-----------|
| App entry | `NavHost` with `Route.Calendar` as sole composable destination |
| Calendar ↔ Edit | `ListDetailPaneScaffold` + local `selectedDate: String?` state in `AdaptiveCalendarScreen` |
| Edit ViewModel key | `dateKey` string (`LocalDate.toString()`) |

When adding a truly separate destination, extend `Route` and register a new `composable<Route.X>` — but prefer the adaptive list/detail pattern for master-detail flows.

### Adaptive layouts

`AdaptiveCalendarScreen` uses Material 3 Adaptive:

```kotlin
val adaptiveInfo = currentWindowAdaptiveInfo()
val directive = calculatePaneScaffoldDirective(adaptiveInfo)
val isSideBySide = directive.maxHorizontalPartitions > 1
```

- **`ListDetailPaneScaffold`**: list pane = calendar, detail pane = edit.
- **`AnimatedPane`**: wrap each pane's content.
- **`calculateThreePaneScaffoldValue`**: when no date selected, force single pane (`maxHorizontalPartitions = 1`).
- **`BackHandler`**: dismisses detail selection on back press.

Behavior differs by layout:

- **Phone (stacked)**: save closes edit pane (`selectedDate = null`).
- **Side-by-side**: save shows Toast, pane stays open.

### Calendar layout modes (within list pane)

`CalendarScreen` chooses layout via **`BoxWithConstraints`** on the available pane size:

```kotlin
val useVerticalMonthScroll = maxHeight > maxWidth && maxWidth < 600.dp
```

| Condition | UI |
|-----------|-----|
| Portrait + width < 600 dp | `LazyColumn` of months with `HorizontalDivider` between them |
| Otherwise | `HorizontalPager` with one month per page |

Both modes share:

- `monthIndexFor` / `monthAtIndex` helpers centered on `YearMonth.now()`
- `PAGER_PAGE_COUNT` = ±50 years of months
- Bidirectional sync between visible month and `CalendarState.selectedMonth` (dropdowns, Today button)

**Important:** `MonthView` uses a fixed `Column`/`Row` grid (not `LazyVerticalGrid`) so it can live inside `LazyColumn` items without nested scrolling conflicts.

Pager state and `LaunchedEffect` sync live **only** in `CalendarHorizontalPager`. Vertical mode sync lives **only** in `CalendarVerticalMonthList`. Do not hoist pager state to `CalendarScreen` when both modes exist.

---

## Data layer

### Room

- Entity: `HeadacheEntry` — `@PrimaryKey date: String` (`YYYY-MM-DD`)
- DAO exposes `Flow<List<HeadacheEntry>>` for reactive reads; suspend functions for one-shot reads/writes
- Database version 2; migrations in `data/Migrations.kt` (e.g. `MIGRATION_1_2` adds `pillsTaken`)
- Singleton via double-checked locking in `HeadacheDatabase.getDatabase()`

Calendar maps entries: `entries.associateBy { it.date }` for O(1) day lookup.

### Backup format

`HeadacheBackup` JSON with `version`, `exportedAt`, `entries[]`. Import validates dates (`LocalDate.parse`), intensity 0–3, pills 0–2. Mismatched version returns `BackupResult.Error`.

Activity contracts (`CreateDocument`, `OpenDocument`) are registered in **`Navigation.kt`**, not in screens.

---

## UI conventions

### Material 3

- Use `MaterialTheme.colorScheme`, `MaterialTheme.typography`, `MaterialTheme.shapes`
- `HeadacheTrackerTheme` enables dynamic color on Android 12+; static light/dark schemes fallback
- Intensity colors are **semantic constants** in `Color.kt`, not derived from the color scheme
- Edge-to-edge enabled in `MainActivity` via `enableEdgeToEdge()`

### Screen structure

- Top-level screens use `Scaffold` with `TopAppBar` where appropriate
- User-facing strings belong in `res/values/strings.xml` (backup messages already do)
- Previews: `@Preview` composables at bottom of screen files, wrapped in `HeadacheTrackerTheme`

### Composable visibility

- Public: top-level screens (`CalendarScreen`, `EditScreen`) and reusable pieces (`DayCell`, `MonthView`)
- Private: layout-mode helpers (`CalendarHorizontalPager`, `CalendarVerticalMonthList`)

---

## Adding a new feature (checklist)

1. **Model** — entity/DTO changes in `model/`; bump DB version + migration if needed.
2. **DAO** — new queries; prefer `Flow` for UI-visible data.
3. **ViewModel** — immutable `XxxState`, `@Inject`, expose `StateFlow`.
4. **Screen** — stateless Composable(s) under `ui/<feature>/`.
5. **DI** — register ViewModel in `AppViewModelFactory`.
6. **Navigation** — wire in `Navigation.kt`: collect state, pass callbacks, handle activity results / adaptive panes here.
7. **Strings** — add to `strings.xml` for user-visible text.

Keep diffs focused. Match naming and patterns in neighboring files.

---

## Build & tooling

```bash
./gradlew assembleDebug    # compile
./gradlew test             # unit tests (minimal coverage today)
```

- **KSP** for Room codegen
- **Compose BOM** pins Compose library versions (`gradle/libs.versions.toml`)
- **compileSdk / targetSdk**: 35; **minSdk**: 26
- Version catalog: `gradle/libs.versions.toml` (dependencies); `self.versions.toml` (the app's own version — single source of truth)

### Versioning & releases

This repo follows the episode6 app-repo shape (see `RELEASE_CHECKLIST.md`, the source of truth):

- The app version lives in `self.versions.toml` (`MAJOR.MINOR.PATCH`, plain numeric). The android versionCode is **derived** in the root `build.gradle.kts` — never set versionCode/versionName by hand.
- Every build is a **snapshot** except CI builds off a release tag: snapshots install side-by-side with the release app under `com.episode6.headachetracker.snapshot` with a ` (SNAPSHOT)` display-name suffix, and derive their versionCode from the git commit count (full history required — shallow clones fail the build). Debug builds additionally append a `.debug` applicationIdSuffix, so a local `installDebug` never clobbers an installed CI-built snapshot APK.
- Every code change needs a `CHANGELOG.md` (or other docs) update — enforced by the `verify-docs` CI workflow. Add bullets under the top `### v<next> - Unreleased` section.
- Releases ship a signed APK to a GitHub release via `build-installers.yml`; the process is automated by the agent skills in `.agents/` (`release-branch-skill`, `ship-release-skill`, `update-docs-skill`, `verify`).

---

## Testing status

The repo has placeholder `ExampleUnitTest` / `ExampleInstrumentedTest` only. There is no established test pattern yet. If adding tests:

- Unit-test ViewModels with a fake DAO and `kotlinx-coroutines-test`
- Prefer testing state transitions and backup validation logic first

---

## Common pitfalls

| Pitfall | Guidance |
|---------|----------|
| INTERNET permission | Never add it (or any network dependency). The app is offline by spec; open URLs in the browser instead. |
| Nested lazy lists | Never put `LazyVerticalGrid` inside `LazyColumn`. Use fixed row/column layouts for month grids. |
| Pager/list sync loops | Guard `onMonthChanged` / scroll updates with `if (selectedMonth != target)` before calling callbacks. |
| ViewModel in Composables | Always pass `viewModelFactory` from `context.appGraph`; use `key =` for parameterized VMs. |
| Date format | Store and pass dates as `LocalDate.toString()` (`YYYY-MM-DD`) consistently. |
| Unused routes | `Route.EditEntry` exists for type-safe nav extensibility; edit currently uses adaptive pane state instead. |
| Gradle deps | New dependencies must appear in `THIRD_PARTY_LICENSES.md` (grouped by license), which is embedded into the app at build time. They must also be added to `app/expected-dependencies.txt` (regenerate with `./gradlew :app:writeExpectedDependencies`) or `:app:verifyReleaseDependencies` fails the build. |

---

## Code style expectations

- Kotlin idioms: data classes for state, sealed interfaces for results/events
- Prefer `combine` / `stateIn` over manual collection for derived UI state
- Constants at file top or `companion object`; magic numbers for calendar bounds already use named private constants
- No comments for obvious code; comment non-obvious business rules (e.g. year dropdown windowing in `CalendarViewModel`)
- Do not commit secrets, `.env`, or local IDE paths

---

## Quick reference: key files

| File | Why it matters |
|------|----------------|
| `ui/navigation/Navigation.kt` | Composition root, adaptive scaffold, VM wiring, file pickers |
| `ui/calendar/CalendarScreen.kt` | Dual calendar layouts, day/month UI |
| `ui/calendar/CalendarViewModel.kt` | Month selection, entries map, export/import |
| `ui/edit/EditViewModel.kt` | AssistedInject example for date-scoped VM |
| `di/AppGraph.kt` | Metro graph definition |
| `di/AppViewModelFactory.kt` | ViewModel creation + `EditDateKey` |
| `data/HeadacheBackupManager.kt` | JSON export/import logic |
| `ui/theme/Color.kt` | Intensity color mapping |
