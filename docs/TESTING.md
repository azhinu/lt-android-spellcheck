# Testing

## Before every installation

Run in the persistent `languagetool-build` container:

```shell
./scripts/build-verified-apk.sh
```

The script stops on the first unit-test, lint, or build failure. Install the APK
only after the script completes successfully.

With a device connected through ADB, run the instrumented Compose UI suite:

```shell
./scripts/run-device-tests.sh
```

## Automated coverage

- Endpoint normalization and rejection of invalid schemes, credentials, queries,
  fragments, paths, and empty values.
- Successful HTTP responses, HTTP 429, malformed JSON, and timeouts.
- LanguageTool request parameters and response parsing.
- Case-insensitive spelling-only dictionary exclusions.
- Merging words stored by Android/Gboard into the application dictionary.
- Plain-text dictionary import/export, including BOM, blank lines, and duplicates.
- Settings persistence and the 500-entry in-memory log limit.
- Android spelling/grammar suggestion ranges, attributes, cookies, and sequences.
- Safe text replacement and zero-length punctuation insertion.
- Unique language codes for every supported language.
- Primary navigation and visibility of each destination.
- Preferred-language dialog opening and search filtering.
- Dictionary actions being confined to the overflow menu.
- Invalid endpoint rejection from the **Save** action without a network request.

## Manual device checks

### Android and Gboard

- The service is listed in Android's spell-checker settings with 33 subtypes.
- The setup prompt disappears from the app after LanguageTool is selected.
- Gboard underlines spelling, grammar, and punctuation errors and shows replacements.
- Updating with `adb install -r` preserves the endpoint, language settings, and dictionary.

Android can retain a dead service binding after replacing an APK. If the service is
selected but Gboard shows no underlines, reboot the device before testing again.

### Endpoint and diagnostics

- **Save** validates the URL, performs a real request, and saves only on success.
- **Test** performs the same validation and request without changing the saved endpoint.
- HTTP, HTTPS with a custom port, timeout, TLS, HTTP 4xx/5xx, and malformed JSON cases
  produce readable runtime log entries.
- Logs disappear after the application process is stopped.

### Languages

- Tapping **Preferred languages** opens the selector directly.
- Search filters both preferred-language and mother-tongue lists by name and code.
- Auto-detection, mother tongue, preferred variants, and the language limit reach the server.

### Dictionary

- A word added from Android/Gboard appears after returning to the app.
- Adding or deleting a word in the app also updates Android's personal dictionary.
- Import, export, and clear actions are available from the Dictionary overflow menu.
- Imported words merge with existing words; exported files contain one word per line.
- Dictionary words suppress spelling matches only, not grammar or punctuation matches.

## Verified on Pixel 8 / Android 16

- English UI and IconKitchen adaptive launcher icon.
- Selected-spell-checker status without the setup prompt.
- Preferred-language dialog and search.
- Real custom HTTP endpoint verification through **Save**.
- Android Personal Dictionary → application dictionary synchronization.
- Runtime diagnostics, correction application, and temporary match hiding.
