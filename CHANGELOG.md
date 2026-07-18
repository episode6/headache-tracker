# Headache Tracker Changelog

### v1.0.10 - Unreleased

- Added a "Check for updates" item to the calendar's 3-dots menu. It opens the
  browser — the main-branch commit log for snapshot builds, the latest GitHub
  release page for release builds — since the app has no network access of its
  own.
- Removed the unused `INTERNET` permission (leftover template boilerplate from
  the initial import). The app is fully offline by spec and must never request
  it.
- Internal: new build checks (wired into `check`, so they run in CI) pin the
  release APK's exact dependency set to `app/expected-dependencies.txt` and its
  merged-manifest permissions to an allowlist, so no library or manifest change
  can introduce network access (or any new dependency/permission) unnoticed.
- Debug builds now use a yellow launcher-icon background (overriding both the purple
  release background and the dark-charcoal snapshot background) so debug installs are
  distinguishable at a glance.
- The Notes summary now opens in the adaptive detail pane (like the edit-day
  screen) instead of a separate navigation destination: on large/unfolded
  devices it shows side-by-side with the calendar; on phones it still covers
  the calendar full-screen.

- While the Notes summary is visible side-by-side with the calendar, days that
  have notes attached get a high-contrast border in the calendar so they're easy
  to spot — dark navy in the light theme, white in the dark theme (the today
  marker still takes precedence). The severity cells in the Notes summary list
  carry the same border so the two screens visually match.

- While the Notes summary is visible side-by-side with the calendar, tapping a
  row in the summary smooth-scrolls the calendar to reveal that entry's month;
  the tapped day's cell pinpoints itself with a combined grow/shrink pulse and
  diagonal glare sweep that starts as the cell scrolls into view and plays
  twice in a row.

- Tapping a Notes summary row while a previous tap's reveal animation is still
  running (including a re-tap of the same row) now interrupts the in-flight
  scroll and day-cell emphasis animation and starts the new reveal immediately,
  instead of the taps being ignored or the animations clobbering each other.

- Internal: replaced the hand-written `AppViewModelFactory` with Metro's
  `metrox-viewmodel` / `metrox-viewmodel-compose` artifacts. ViewModels are now
  contributed to the DI graph via `@ViewModelKey` / `ManualViewModelAssistedFactory`
  multibindings and obtained in Compose with `metroViewModel()` /
  `assistedMetroViewModel()`; no user-facing changes.

- Added a Notes summary screen (calendar 3-dots menu → Notes summary): a vertical
  list of only the days that have notes, segmented by year. Each row shows the
  month + day, a combined severity/pill-count indicator (the same colored square +
  pill dots used by the calendar), and the note text. The list opens anchored to
  the bottom (the most recent note), so you scroll up to read into the past.

- Fixed the edit-day pane blanking out before its exit transition finished when
  backing out (or saving) on phones — the pane now keeps its content while it
  animates away.
- Moved Export data, Import data, and Auto-Export from the calendar's 3-dots menu
  to the Settings screen, which is now organized into grouped sections (Reminders,
  Backup & data, About) with a refreshed Material 3 look: section cards, per-row
  icons, and inline supporting text (including the last auto-export time under the
  Auto-Export toggle). Export/import progress and result snackbars now show on the
  Settings screen. The calendar's 3-dots menu is down to Full year view and
  Settings.
- Added a Third-party license notices screen (Settings → Third-party license
  notices). The notices live in `THIRD_PARTY_LICENSES.md` at the repo root and are
  embedded into the app at build time, so the in-app screen always matches the
  document the repo ships.
- Removed unused dependencies (Retrofit, Moshi, OkHttp + logging-interceptor,
  Coil, CameraX, Play Services Location), shrinking the APK and the third-party
  license notices.

- Tapping the 2nd-pill reminder notification now opens the app directly to
  today's edit screen (matching the morning check-in's tap behavior).
- Added a morning check-in notification that asks each morning whether you had a
  headache yesterday; tapping it opens the app directly to yesterday's edit screen.
  Configurable on the Settings screen: an on/off toggle (default on) and a time-of-day
  picker (default 8:00 AM local).
- Both reminder notifications are scheduled with exact alarms (AlarmManager +
  `USE_EXACT_ALARM`/`SCHEDULE_EXACT_ALARM`) so they fire on time even in Doze mode,
  falling back to inexact alarms if the Alarms & Reminders access is revoked. A boot
  receiver re-arms both reminders after reboots and app updates.
- Added a 2nd-pill reminder: saving today's entry with 1 pill taken now schedules a
  local notification N minutes after the first pill was taken (via WorkManager).
  Saving today with 0 or 2 pills cancels the pending reminder; editing past days
  never touches it. N is configurable on the new Settings screen (reachable from
  the calendar's 3-dots menu, default 60, clamped to 45–150 minutes), which also
  links to the system's per-app notification settings. The app now declares
  POST_NOTIFICATIONS and requests it on launch (Android 13+).
- CI: after building the APK, the Build Installers workflow now comments on the
  triggering PR (or commit, for pushes) with a download link for the APK artifact
  plus a QR code for installing it on a device. QR images are committed to the
  `episode6/qrcodes` repo and hot-linked via raw URLs (requires a
  `QRCODES_GITHUB_TOKEN` secret; without it the comment posts link-only).
- Restyled the pill indicator dots on the calendar and full-year screens: slightly
  larger white dots with a thin dark-navy ring, so they're easier to distinguish
  against the colored intensity backgrounds. Also added a little more space between
  the dots on two-pill days, and nudged the dots slightly further below the day
  number on the monthly calendar.

### v1.0.0 - 2026-07-14

- Committed a shared debug keystore (`debug.keystore` in the repo root, standard
  debug credentials) and pointed the debug signing config at it, so CI-built and
  local debug APKs share a signature and can overwrite each other on-device.
- Shrunk the calendar artwork in the launcher icon (to 80%) so it fits inside the
  circular safe zone of Android's adaptive icon — previously the round mask clipped
  all four corners of the calendar.
- Import the episode6 app-repo CI/release infrastructure (from podcast-hacker):
  versioning via `self.versions.toml` with derived versionCodes, snapshot vs release
  build identities (snapshots install side-by-side as "Headache Tracker (SNAPSHOT)" /
  `com.episode6.headachetracker.snapshot`), GitHub Actions workflows (APK builds +
  release attachment, emulator device tests, docs/changelog enforcement, version sync,
  no-snapshot-deps guard), release scripts, and in-repo agent skills.
- Snapshot builds carry their own launcher icon (dark charcoal background; releases
  keep the purple one) so the two side-by-side installs are distinguishable at a
  glance.
- Release builds are now minified and resource-shrunk with R8 (full mode, the AGP
  default), significantly shrinking the APK.
- On the day entry screen, tapping a severity level smooth-scrolls to the bottom of
  the screen, revealing the pills section.
- Debug builds now install under their own applicationId (a `.debug` suffix) so they
  coexist with CI-built snapshot APKs instead of clobbering them.
- The day entry screen has a new optional notes section (a 3-line text box below the
  pill selection). Notes are stored with the entry and included in exports/imports.
- Updated the Compose BOM from 2024.09.00 to 2026.06.01 (Compose UI/foundation
  1.7.0 → 1.11.4, plus the matching Material 3 release). Locale and string-resource
  reads in composables now go through observable APIs (`LocalLocale`,
  `LocalResources`) per the BOM's new lint checks, so month names and UI strings
  update immediately if the system locale changes.
