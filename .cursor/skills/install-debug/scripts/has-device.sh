#!/usr/bin/env bash
# Exit 0 if adb reports at least one device in "device" state (ready).
adb devices 2>/dev/null | awk 'NR>1 && $2 == "device" { found=1 } END { exit !found }'
