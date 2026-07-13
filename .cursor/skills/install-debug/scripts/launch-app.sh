#!/usr/bin/env bash
# Launch the default launcher activity on every adb device in "device" state.
set -euo pipefail

# local builds are always snapshots (see selfAppId in the root build.gradle.kts);
# the activity class keeps the base package — it follows the fixed namespace
PACKAGE="com.episode6.snapshots.headachetracker"
ACTIVITY="${PACKAGE}/com.episode6.headachetracker.MainActivity"

serials=$(adb devices 2>/dev/null | awk 'NR>1 && $2 == "device" { print $1 }')
if [ -z "$serials" ]; then
  echo "No ready devices for launch." >&2
  exit 1
fi

while IFS= read -r serial; do
  echo "Launching on ${serial}..."
  adb -s "$serial" shell am start -n "$ACTIVITY" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER
done <<< "$serials"
