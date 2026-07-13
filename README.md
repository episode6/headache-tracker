# Headache Tracker

A local-first Android app for logging daily headache severity on a color-coded calendar. Built with Jetpack Compose and Material Design 3, with adaptive layouts for phones, foldables, and tablets.

## Features

- **Monthly calendar** — days are color-coded by intensity (0 = green, 1–3 = yellow → red)
- **Quick logging** — tap a day to set intensity (0–3) and pills taken (0–2)
- **Navigation** — swipe between months on wide/landscape layouts; scroll vertically through months on portrait phones
- **Export / import** — back up entries as JSON via the system file picker
- **Adaptive UI** — calendar and edit pane appear side-by-side on foldables and large screens

All data stays on device in a SQLite database (Room). No account or network required for core functionality.

## Tech stack

| Layer | Choice |
|-------|--------|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Adaptive layouts | Compose Material Adaptive (`ListDetailPaneScaffold`) |
| Navigation | Navigation Compose with type-safe `@Serializable` routes |
| Persistence | Room |
| DI | [Metro](https://github.com/ZacSweers/metro) |
| Async | Kotlin Coroutines & Flow |

## Requirements

- Android Studio Ladybug or newer (or compatible IDE)
- JDK 11+
- Android SDK 35
- minSdk 26

## Build & run

```bash
./gradlew assembleDebug
```

Install the debug APK from `app/build/outputs/apk/debug/`, or run from Android Studio on a device/emulator.

## Project layout

```
app/src/main/java/com/episode6/headachetracker/
├── data/          Room database, DAO, migrations, JSON backup
├── di/            Metro graph and ViewModel factory
├── model/         Room entities and backup DTOs
└── ui/
    ├── calendar/  Main calendar screen + ViewModel
    ├── edit/      Entry editor screen + ViewModel
    ├── navigation/ NavHost and adaptive list/detail wiring
    └── theme/     Material 3 colors, typography, theme
```

## Architecture (short)

Screens are **stateless Composables** that receive immutable state and callbacks. **ViewModels** expose `StateFlow` UI state and talk to the DAO. **Navigation** owns cross-cutting concerns (file pickers, adaptive pane state, ViewModel scoping). **Metro** wires dependencies at app scope.

See [AGENTS.md](AGENTS.md) for detailed conventions aimed at contributors and AI assistants.
