# Headache Tracker Changelog

### v1.0.10 - Unreleased

- Added a morning check-in notification that asks each morning whether you had a
  headache yesterday; tapping it opens the app directly to yesterday's edit screen.
  Configurable on the Settings screen: an on/off toggle (default on) and a time-of-day
  picker (default 8:00 AM local). Scheduled via a self-rechaining WorkManager job.
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
