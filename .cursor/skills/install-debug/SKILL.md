---
name: install-debug
description: >-
  Installs the debug APK on a connected Android device after app changes using
  ./gradlew installDebug, then launches the app. Use after editing app code,
  resources, or Gradle app config in this project, when finishing implementation
  tasks, or when the user asks to deploy or test on a device.
---

# Install Debug After Changes

After any change that affects the runnable app, **install it on a connected device and launch it** before you finish the turn.

## When this applies

Run this workflow when you changed any of:

- `app/src/**` (Kotlin, Compose, assets)
- `app/src/main/res/**`
- `app/build.gradle.kts` or root `build.gradle.kts` / `gradle/**` when it affects the app module

**Skip** when the session was read-only, or you only changed docs, CI config unrelated to the APK, or files outside the Android app.

`assembleDebug` alone is **not** enough when a device is connected — you must run **`installDebug`** and **launch the app**.

## Workflow

Copy and track:

```
Install debug:
- [ ] 1. Check for a connected device
- [ ] 2. If connected: run ./gradlew installDebug
- [ ] 3. Launch the default activity on connected device(s)
- [ ] 4. Confirm success or fix failures
```

### 1. Check for a connected device

From the **repository root**:

```bash
.cursor/skills/install-debug/scripts/has-device.sh
```

- Exit code **0** → at least one device is ready (`adb` state `device`). Continue to step 2.
- Exit code **non-zero** → no ready device. **Do not** run `installDebug` or launch. Note briefly in your reply that install was skipped (no device).

If `adb` is missing from PATH, say so and skip install.

**Multiple devices:** `installDebug` and `launch-app.sh` target **every** device in the `device` state. To limit to one device, set `ANDROID_SERIAL` before Gradle/adb commands, or ask the user which device to use.

### 2. Install

From the repository root:

```bash
./gradlew installDebug
```

Use a long enough timeout (install + build can take a minute or more). Do not proceed to launch until install succeeds.

### 3. Launch

From the repository root:

```bash
.cursor/skills/install-debug/scripts/launch-app.sh
```

This starts `com.episode6.headachetracker/.MainActivity` (the `MAIN` / `LAUNCHER` activity). Run it **after every successful** `installDebug` in this workflow.

If launch fails but install succeeded, report the adb error; the APK is still on the device.

### 4. On failure

- Read the Gradle/adb error output.
- Fix the underlying issue (compile error, signing, insufficient storage, unauthorized device, etc.).
- Re-run from step 2 (`installDebug`, then `launch-app.sh`) after fixing.

## Reporting

When a device was connected and install + launch succeeded, mention that the debug build was installed and opened (one short line is enough).

When skipped (no device), no need to run Gradle for install — but you may still use `assembleDebug` or `test` if the user asked for verification.

## Package reference

| Value | Source |
|-------|--------|
| `applicationId` | `selfAppId` in root `build.gradle.kts` → `com.episode6.snapshots.headachetracker` for local (snapshot) builds |
| Launcher activity | `MainActivity` in `AndroidManifest.xml` (class stays in the fixed `com.episode6.headachetracker` namespace) |

If either changes, update `scripts/launch-app.sh`.
