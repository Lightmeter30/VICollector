#!/usr/bin/env bash
set -euo pipefail

LOCAL_ROOT="${1:-./data}"
RUN_LABEL="${2:-batch_$(date +%Y%m%d_%H%M%S)}"
DEVICE_PATH="/sdcard/Android/data/com.example.vicollector/files/VICollectorDataset"
RUN_DIR="${LOCAL_ROOT}/${RUN_LABEL}"

mkdir -p "${RUN_DIR}"
adb devices
adb pull "${DEVICE_PATH}" "${RUN_DIR}/"
echo "Pulled VICollectorDataset into ${RUN_DIR}."
