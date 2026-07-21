package azhinu.languagetool.android.model

data class SupportedLanguage(val code: String, val name: String)

object SupportedLanguages {
    val all = listOf(
        SupportedLanguage("ast", "Asturian"), SupportedLanguage("br", "Breton"),
        SupportedLanguage("ca", "Catalan"), SupportedLanguage("da", "Danish"),
        SupportedLanguage("de", "German"), SupportedLanguage("en", "English"),
        SupportedLanguage("es", "Spanish"), SupportedLanguage("eo", "Esperanto"),
        SupportedLanguage("fr", "French"), SupportedLanguage("ga", "Irish"),
        SupportedLanguage("gl", "Galician"), SupportedLanguage("it", "Italian"),
        SupportedLanguage("nl", "Dutch"), SupportedLanguage("no", "Norwegian"),
        SupportedLanguage("nb", "Norwegian Bokmål"), SupportedLanguage("pl", "Polish"),
        SupportedLanguage("pt", "Portuguese"), SupportedLanguage("ro", "Romanian"),
        SupportedLanguage("sl", "Slovenian"), SupportedLanguage("sk", "Slovak"),
        SupportedLanguage("sv", "Swedish"), SupportedLanguage("tl", "Tagalog"),
        SupportedLanguage("uk", "Ukrainian"), SupportedLanguage("el", "Greek"),
        SupportedLanguage("be", "Belarusian"), SupportedLanguage("crh", "Crimean Tatar"),
        SupportedLanguage("ru", "Russian"), SupportedLanguage("ar", "Arabic"),
        SupportedLanguage("fa", "Persian"), SupportedLanguage("ta", "Tamil"),
        SupportedLanguage("km", "Khmer"), SupportedLanguage("zh", "Chinese"),
        SupportedLanguage("ja", "Japanese")
    )

    val variants = linkedMapOf(
        "ca" to listOf("ca-ES", "ca-ES-balear", "ca-ES-valencia"),
        "de" to listOf("de-DE", "de-LU", "de-AT", "de-CH"),
        "en" to listOf("en-US", "en-GB", "en-AU", "en-CA", "en-NZ", "en-ZA"),
        "es" to listOf("es-ES", "es-AR"),
        "fr" to listOf("fr-FR", "fr-BE", "fr-CA", "fr-CH"),
        "nl" to listOf("nl-NL", "nl-BE"),
        "pt" to listOf("pt-PT", "pt-AO", "pt-BR", "pt-MZ")
    )

    val defaultVariants = mapOf(
        "ca" to "ca-ES", "de" to "de-DE", "en" to "en-US", "es" to "es-ES",
        "fr" to "fr-FR", "nl" to "nl-NL", "pt" to "pt-PT"
    )
}

data class LanguageToolSettings(
    val endpoint: String = "https://api.languagetool.org",
    val motherTongue: String? = null,
    val preferredLanguages: Set<String> = setOf("en"),
    val forcePreferredLanguages: Boolean = false,
    val preferredVariants: Map<String, String> = SupportedLanguages.defaultVariants,
    val pickyMode: Boolean = false,
    val dictionary: Set<String> = emptySet(),
    val ignoredRuleIds: Set<String> = emptySet()
)

data class LanguageToolMatch(
    val message: String,
    val shortMessage: String,
    val offset: Int,
    val length: Int,
    val replacements: List<String>,
    val ruleId: String,
    val issueType: String,
    val categoryId: String
) {
    val isSpelling: Boolean
        get() = issueType.equals("misspelling", true) || categoryId.equals("TYPOS", true)
}

data class LanguageToolResult(
    val detectedLanguage: String?,
    val matches: List<LanguageToolMatch>
)
