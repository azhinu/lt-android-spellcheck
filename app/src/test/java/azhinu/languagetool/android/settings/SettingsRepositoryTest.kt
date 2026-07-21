package azhinu.languagetool.android.settings

import android.content.Context
import azhinu.languagetool.android.model.LanguageToolSettings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SettingsRepositoryTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Before
    @After
    fun clearPreferences() {
        context.getSharedPreferences("languagetool_settings", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun savesAndLoadsEverySetting() {
        val repository = SettingsRepository(context, systemWords = { emptySet() })
        val expected = LanguageToolSettings(
            endpoint = "http://192.168.1.2:8081",
            motherTongue = "ru",
            preferredLanguages = setOf("ru", "en"),
            forcePreferredLanguages = true,
            preferredVariants = mapOf(
                "ca" to "ca-ES-valencia", "de" to "de-CH", "en" to "en-GB",
                "es" to "es-AR", "fr" to "fr-CA", "nl" to "nl-BE", "pt" to "pt-BR"
            ),
            pickyMode = true,
            dictionary = setOf("Codex", "Ажину"),
            ignoredRuleIds = setOf("RULE_ONE", "RULE_TWO")
        )

        repository.save(expected)

        assertEquals(expected, repository.load())
    }

    @Test
    fun mergesWordsAddedThroughAndroidIntoTheApplicationDictionary() {
        val repository = SettingsRepository(context, systemWords = { setOf("GboardWord") })
        repository.save(LanguageToolSettings(dictionary = setOf("ApplicationWord")))

        assertEquals(setOf("ApplicationWord", "GboardWord"), repository.load().dictionary)
    }
}
