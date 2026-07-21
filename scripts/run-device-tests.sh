#!/usr/bin/env sh
set -eu

ADB_COMMAND=${ADB_COMMAND:-adb}
APP_APK=app/build/outputs/apk/debug/app-debug.apk
TEST_APK=app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk

if [ ! -f "$APP_APK" ] || [ ! -f "$TEST_APK" ]; then
  printf '%s\n' "Debug APKs are missing. Run scripts/build-verified-apk.sh first." >&2
  exit 1
fi

"$ADB_COMMAND" install --no-incremental -r "$APP_APK"
"$ADB_COMMAND" install --no-incremental -r -t "$TEST_APK"
"$ADB_COMMAND" shell input keyevent KEYCODE_WAKEUP
"$ADB_COMMAND" shell wm dismiss-keyguard
set +e
INSTRUMENT_OUTPUT=$("$ADB_COMMAND" shell am instrument -w \
  azhinu.languagetool.android.test/androidx.test.runner.AndroidJUnitRunner)
INSTRUMENT_STATUS=$?
set -e
printf '%s\n' "$INSTRUMENT_OUTPUT"

if [ "$INSTRUMENT_STATUS" -ne 0 ]; then
  printf '%s\n' "Instrumented test runner failed." >&2
  exit "$INSTRUMENT_STATUS"
fi

case "$INSTRUMENT_OUTPUT" in
  *"FAILURES!!!"*)
    printf '%s\n' "Instrumented tests failed." >&2
    exit 1
    ;;
  *"OK ("*)
    ;;
  *)
    printf '%s\n' "Instrumented tests did not report a successful result." >&2
    exit 1
    ;;
esac

printf '%s\n' "Rebooting the device to restore the system spell checker after APK replacement."
"$ADB_COMMAND" reboot
"$ADB_COMMAND" wait-for-device

BOOT_ATTEMPTS=0
while [ "$("$ADB_COMMAND" shell getprop sys.boot_completed | tr -d '\r')" != "1" ]; do
  BOOT_ATTEMPTS=$((BOOT_ATTEMPTS + 1))
  if [ "$BOOT_ATTEMPTS" -ge 120 ]; then
    printf '%s\n' "Device did not finish booting after 120 seconds." >&2
    exit 1
  fi
  sleep 1
done

printf '%s\n' "Device rebooted. Unlock it before manually testing the system spell checker."
