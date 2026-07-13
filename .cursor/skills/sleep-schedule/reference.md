# Sleep Schedule — Reference Checklist

Use this when auditing docs against the codebase. Re-read source files if any item is uncertain.

## Product & data model

| Fact | Source |
|------|--------|
| One entry per day, PK `date: String` (`YYYY-MM-DD`) | `model/HeadacheEntry.kt` |
| `intensity`: 0–3 | `HeadacheEntry.kt`, edit UI |
| `pillsTaken`: 0–2, default 0 | `HeadacheEntry.kt`, `MIGRATION_1_2` |
| Room DB version | `HeadacheDatabase.kt` `@Database(version = …)` |
| Migrations | `data/Migrations.kt` |
| Backup JSON: version, exportedAt, entries | `model/HeadacheBackup.kt`, `HeadacheBackupManager.kt` |
| Import validation rules | `HeadacheBackupManager.kt` |
| No backend / network for core features | App architecture |

## Package map

Verify directories under `app/src/main/java/com/episode6/headachetracker/`:

| Package | Expected contents |
|---------|-------------------|
| `data/` | Database, DAO, migrations, backup manager |
| `di/` | `AppGraph`, `AppViewModelFactory` |
| `model/` | Entity, backup DTOs |
| `ui/calendar/` | Calendar screen + ViewModel |
| `ui/edit/` | Edit screen + ViewModel |
| `ui/navigation/` | NavHost, adaptive wiring, routes |
| `ui/theme/` | Theme, colors, typography |

Root: `HeadacheTrackerApp`, `MainActivity`, `Context.appGraph` extension.

## Architecture patterns

| Pattern | Key files |
|---------|-----------|
| Stateless Composables + callbacks | `CalendarScreen.kt`, `EditScreen.kt` |
| ViewModel `StateFlow` / `SharedFlow` events | `*ViewModel.kt` |
| State collection in navigation layer | `Navigation.kt` |
| Metro `@DependencyGraph` app scope | `AppGraph.kt` |
| AssistedInject edit VM with date key | `EditViewModel.kt`, `AppViewModelFactory.kt` |
| Type-safe routes | `Routes.kt` |
| Hybrid nav: NavHost + `ListDetailPaneScaffold` | `Navigation.kt` |
| File picker contracts in navigation | `Navigation.kt` |
| Calendar vertical vs pager layout | `CalendarScreen.kt` |
| Fixed grid in `MonthView` (no nested lazy grid) | `CalendarScreen.kt` |

## UI & theme

| Fact | Source |
|------|--------|
| Intensity colors `Intensity0`–`Intensity3` | `ui/theme/Color.kt` |
| Dynamic color on Android 12+ | `ui/theme/Theme.kt` |
| Edge-to-edge | `MainActivity.kt` |
| Pills shown as dots on day cells | `CalendarScreen.kt` |

## Build & tooling

| Fact | Source |
|------|--------|
| minSdk / compileSdk / targetSdk | `app/build.gradle.kts` |
| JDK version | `compileOptions` in `app/build.gradle.kts` |
| KSP for Room (+ Metro) | `app/build.gradle.kts`, plugins |
| Compose BOM | `gradle/libs.versions.toml` |
| Test commands | `./gradlew assembleDebug`, `./gradlew test` |

## Intentionally undocumented in README

- Unused deps (Retrofit, Camera, Location, etc.) — mention only in AGENTS pitfalls
- ViewModel registration steps — AGENTS only
- Calendar sync loop guards — AGENTS only

## Repo-local skills inventory

After adding skills, list them here mentally and ensure each has valid `SKILL.md`:

```
.cursor/skills/
└── sleep-schedule/
    ├── SKILL.md
    └── reference.md
```

When new skills are added, verify their descriptions do not contradict AGENTS.md or README.

## Hooks

| File | Purpose |
|------|---------|
| `.cursor/hooks.json` | Registers stop hook |
| `.cursor/hooks/sleep-schedule.sh` | Triggers doc sync follow-up after relevant changes |

AGENTS.md should mention the Documentation / sleep-schedule maintenance loop briefly.
