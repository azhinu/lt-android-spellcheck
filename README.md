# LanguageTool for Android

A vibe-coded Android application that exposes a LanguageTool-compatible server as the system
spell checker. Once enabled in Android settings, it can provide spelling, grammar, and punctuation
suggestions in applications that support the Android spell-checker framework.

The app supports custom HTTP(S) endpoints, preferred languages and variants, a personal spelling
dictionary, correction suggestions, and in-memory diagnostic logs.

# App updating

Android normally stops the application process, including its spell-checker service, while
replacing an installed APK. The system should reconnect to the updated service when a text field
requests spell checking, and the selected endpoint, languages, and dictionary remain intact.

On Android 16, the system can occasionally retain a dead Binder connection after an update. The
app then remains selected as the system spell checker, but text fields receive no suggestions. If
this happens, select another spell checker and then select **LanguageTool for Android** again. If
that does not restore checking, reboot the device.

## License

[WTFPL](LICENSE)
