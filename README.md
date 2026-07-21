# LanguageTool for Android

A vibe-coded Android application that exposes a LanguageTool-compatible server as the system
spell checker. Once enabled in Android settings, it can provide spelling, grammar, and punctuation
suggestions in applications that support the Android spell-checker framework.

The app supports custom HTTP(S) endpoints, preferred languages and variants, a personal spelling
dictionary, correction suggestions, and in-memory diagnostic logs.

## Setup

1. Install the APK and open the app.
2. Configure and test your LanguageTool endpoint.
3. Open the system spell-checker settings from the app.
4. Enable spell checking and select **LanguageTool for Android**.

Package name: `azhinu.languagetool.android`.

## Build and test

The project uses the persistent `languagetool-build` Docker container with the repository mounted
at `/work`:

```shell
docker exec -w /work languagetool-build \
  ./scripts/build-verified-apk.sh
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. The verification script
runs unit tests, Android lint, and builds the application and instrumented UI tests.

With an Android device connected through ADB:

```shell
./scripts/run-device-tests.sh
```

## Updating the app

Android normally stops the application process, including its spell-checker service, while
replacing an installed APK. The system should reconnect to the updated service when a text field
requests spell checking, and the selected endpoint, languages, and dictionary remain intact.

On Android 16, the system can occasionally retain a dead Binder connection after an update. The
app then remains selected as the system spell checker, but text fields receive no suggestions. If
this happens, select another spell checker and then select **LanguageTool for Android** again. If
that does not restore checking, reboot the device.

For ADB updates, avoid incremental installation:

```shell
adb install --no-incremental -r app.apk
```

The device test script reboots the device automatically after replacing the APK so that manual
spell-checker testing starts with a fresh system connection.

## Notes

- Text is sent to the configured LanguageTool server for checking.
- Programmatic whole-field replacement may not trigger Android spell checking; normal user input does.
- Correction popup styling is controlled by Android or the application containing the text field.
- Runtime logs are kept in memory only and disappear when the application process stops.

The launcher resources in `app/src/main/res` were generated from the IconKitchen asset set supplied
for this application.

## License

[WTFPL](LICENSE)
