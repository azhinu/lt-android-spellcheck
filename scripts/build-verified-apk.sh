#!/usr/bin/env sh
set -eu

./gradlew --no-daemon --console=plain \
  testDebugUnitTest \
  lintDebug \
  assembleDebug \
  assembleDebugAndroidTest

printf '%s\n' "Verified APK: app/build/outputs/apk/debug/app-debug.apk"
printf '%s\n' "Instrumented tests: app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"
