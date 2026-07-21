# LanguageTool for Android

A native Android system spell checker backed by a LanguageTool-compatible HTTP API.

## Features

- Custom HTTP(S) endpoint with format validation and a real server check before saving.
- Automatic detection among preferred languages, mother tongue, and language variants.
- Picky mode for additional punctuation and typography rules.
- Spelling, grammar, and punctuation suggestions through Android `SpellCheckerService`.
- A personal dictionary synchronized with Android/Gboard's user dictionary.
- Plain-text dictionary import and export through Android's system file picker.
- Temporary hiding of a single match and persistent disabling of LanguageTool rules.
- Playground for checking text and applying suggested corrections.
- In-memory diagnostics log that is never persisted to disk.

## Build and test

The project is built in the persistent `languagetool-build` container with the
repository mounted at `/work`. The container does not need to be recreated after a build.

```shell
docker exec -w /work languagetool-build \
  ./scripts/build-verified-apk.sh
```

The verified APK is written to `app/build/outputs/apk/debug/app-debug.apk`.
The verification script runs unit tests, Android lint, and builds both the application and
its instrumented UI tests.

With an Android device connected through ADB, install the fresh build and run the Compose UI
suite with:

```shell
./scripts/run-device-tests.sh
```

The full automated and manual test matrix is available in
[`docs/TESTING.md`](docs/TESTING.md).

## Enable on Android

1. Install the APK and open the app.
2. Tap **Open settings**.
3. Enable the spell checker and select **LanguageTool for Android**.
4. Confirm Android's system warning.
5. Keep spell checking enabled in Gboard.

Package name: `azhinu.languagetool.android`.

## Assets

The Android launcher resources in `app/src/main/res` were generated from the IconKitchen
asset set supplied for this application.
