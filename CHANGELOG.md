# Headache Tracker Changelog

### v1.0.0 - Unreleased

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
