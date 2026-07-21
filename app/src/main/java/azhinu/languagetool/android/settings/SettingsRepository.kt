package azhinu.languagetool.android.settings

import android.content.Context
import azhinu.languagetool.android.dictionary.SystemDictionaryRepository
import azhinu.languagetool.android.model.LanguageToolSettings
import azhinu.languagetool.android.model.SupportedLanguages
import java.util.Locale

class SettingsRepository(
    context: Context,
    private val systemWords: () -> Set<String> = { SystemDictionaryRepository(context).loadWords() }
) {
    private val preferences = context.getSharedPreferences("languagetool_settings", Context.MODE_PRIVATE)

    fun load(): LanguageToolSettings {
        val localeLanguage = Locale.getDefault().language
        val defaultLanguage = localeLanguage.takeIf { code ->
            SupportedLanguages.all.any { it.code == code }
        } ?: "en"

        val variants = SupportedLanguages.defaultVariants.mapValues { (language, fallback) ->
            preferences.getString("variant_$language", fallback) ?: fallback
        }

        return LanguageToolSettings(
            endpoint = preferences.getString("endpoint", DEFAULT_ENDPOINT) ?: DEFAULT_ENDPOINT,
            motherTongue = preferences.getString("mother_tongue", null),
            preferredLanguages = preferences.getStringSet(
                "preferred_languages", setOf(defaultLanguage)
            )?.toSet().orEmpty(),
            forcePreferredLanguages = preferences.getBoolean("force_preferred_languages", false),
            preferredVariants = variants,
            pickyMode = preferences.getBoolean("picky_mode", false),
            dictionary = preferences.getStringSet("dictionary", emptySet())?.toSet().orEmpty() +
                systemWords(),
            ignoredRuleIds = preferences.getStringSet("ignored_rule_ids", emptySet())?.toSet().orEmpty()
        )
    }

    fun save(settings: LanguageToolSettings) {
        preferences.edit()
            .putString("endpoint", settings.endpoint)
            .putString("mother_tongue", settings.motherTongue)
            .putStringSet("preferred_languages", settings.preferredLanguages)
            .putBoolean("force_preferred_languages", settings.forcePreferredLanguages)
            .putBoolean("picky_mode", settings.pickyMode)
            .putStringSet("dictionary", settings.dictionary)
            .putStringSet("ignored_rule_ids", settings.ignoredRuleIds)
            .also { editor ->
                settings.preferredVariants.forEach { (language, variant) ->
                    editor.putString("variant_$language", variant)
                }
            }
            .apply()
    }

    companion object {
        const val DEFAULT_ENDPOINT = "https://api.languagetool.org"
    }
}
