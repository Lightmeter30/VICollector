#!/usr/bin/env bash
set -euo pipefail

DEVICE_PATH="/sdcard/Android/data/com.example.vicollector/files/VICollectorDataset"

adb devices
adb shell "rm -rf '${DEVICE_PATH}'"
echo "Removed ${DEVICE_PATH} from connected device."
