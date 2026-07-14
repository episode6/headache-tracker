# Headache Tracker Changelog

### v1.0.0 - Unreleased

- Import the episode6 app-repo CI/release infrastructure (from podcast-hacker):
  versioning via `self.versions.toml` with derived versionCodes, snapshot vs release
  build identities (snapshots install side-by-side as "Headache Tracker (SNAPSHOT)" /
  `com.episode6.snapshots.headachetracker`), GitHub Actions workflows (APK builds +
  release attachment, emulator device tests, docs/changelog enforcement, version sync,
  no-snapshot-deps guard), release scripts, and in-repo agent skills.
- Snapshot builds carry their own launcher icon (dark charcoal background; releases
  keep the purple one) so the two side-by-side installs are distinguishable at a
  glance.
- Release builds are now minified and resource-shrunk with R8 (full mode, the AGP
  default), significantly shrinking the APK.
- On the day entry screen, tapping a severity level smooth-scrolls to the bottom of
  the screen, revealing the pills section.
- Debug builds now install under their own applicationId
  (`com.episode6.debug.headachetracker`) so they coexist with CI-built snapshot APKs
  instead of clobbering them.
