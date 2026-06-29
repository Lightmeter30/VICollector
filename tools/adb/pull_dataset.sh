#!/usr/bin/env bash
set -euo pipefail

DEVICE_PATH="/sdcard/Android/data/com.example.vicollector/files/VICollectorDataset"
LOCAL_PATH="${1:-./data}"

adb devices
mkdir -p "${LOCAL_PATH}"
adb pull "${DEVICE_PATH}" "${LOCAL_PATH}/"
