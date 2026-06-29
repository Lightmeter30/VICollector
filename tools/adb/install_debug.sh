#!/usr/bin/env bash
set -euo pipefail

./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
