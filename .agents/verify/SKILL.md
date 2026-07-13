---
name: verify
description: How to verify headache-tracker changes end-to-end by driving the real Android app on a device/emulator (install debug build, launch, tap through the calendar/edit flow, screenshots). Use when verifying UI or data changes in this repo, or when asked to run/screenshot the Android app.
---

# Verifying headache-tracker on Android

The app is a single-module Compose app (`app/`). The only real-runtime surface is a
device/emulator; unit tests (`./gradlew test`) cover ViewModel logic but are not a
substitute for driving the app.

## Build + install

```bash
./gradlew :app:installDebug
# local builds are always snapshots, so the applicationId is the snapshot one
# (the activity class keeps the base package — it follows the fixed namespace)
adb shell am start -n com.episode6.snapshots.headachetracker/com.episode6.headachetracker.MainActivity
adb shell pm clear com.episode6.snapshots.headachetracker   # reset to empty state
```

Check for a connected device first (`adb devices` — state must be `device`). With
multiple devices, set `ANDROID_SERIAL` or pass `adb -s <serial>`.

## Core flow to exercise

Calendar (launch screen) → tap a day → edit pane opens (slides in on phones,
side-by-side on wide screens) → set intensity (0–3) and pills (0–2) → save →
day cell recolors by intensity, pills show as dots. Also worth touching when
relevant: month navigation (vertical scroll in portrait, pager in landscape/wide),
full-year view, and export/import (JSON via system file picker).

## Driving + screenshots

- `adb shell input tap X Y`, `adb shell input text '...'`, screenshots via
  `adb exec-out screencap -p > shot.png`.
- Foldables (multiple displays) prepend a warning that corrupts the PNG; list ids with
  `adb shell dumpsys SurfaceFlinger --display-id` and use `screencap -p -d <id>`.
- Landscape probe: `settings put system user_rotation 1` (and back to 0) — the
  calendar switches layout modes on orientation/width, so check both when touching it.
- Gesture nav check: `adb shell settings get secure navigation_mode` (2 = gestures).

## Gotchas

- The snapshot build installs side-by-side with a released install — make sure you're
  looking at "Headache Tracker (SNAPSHOT)", not the release app, when checking changes.
- Entries are keyed by calendar date; "today" moves, so a fresh `pm clear` plus logging
  a known set of days gives reproducible screenshots.
- Export/import uses the system document picker — drive it with taps (it's outside the
  app's UI tree), and pull exported JSON via `adb pull` from the chosen location.
