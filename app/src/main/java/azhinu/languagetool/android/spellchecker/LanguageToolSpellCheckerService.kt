package azhinu.languagetool.android.spellchecker

import android.service.textservice.SpellCheckerService
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import azhinu.languagetool.android.api.LanguageToolClient
import azhinu.languagetool.android.logging.RuntimeLog
import azhinu.languagetool.android.model.LanguageToolMatch
import azhinu.languagetool.android.settings.SettingsRepository

class LanguageToolSpellCheckerService : SpellCheckerService() {
    override fun createSession(): Session = LanguageToolSession()

    inner class LanguageToolSession : Session() {
        private lateinit var settingsRepository: SettingsRepository
        private val client = LanguageToolClient()

        override fun onCreate() {
            settingsRepository = SettingsRepository(applicationContext)
            RuntimeLog.info("System spell checker session created", locale)
        }

        override fun onGetSentenceSuggestionsMultiple(
            textInfos: Array<out TextInfo>,
            suggestionsLimit: Int
        ): Array<SentenceSuggestionsInfo> = textInfos.map { textInfo ->
            getSentenceSuggestions(textInfo, suggestionsLimit)
        }.toTypedArray()

        @Deprecated("Android uses the sentence API starting with API 16")
        override fun onGetSuggestions(textInfo: TextInfo, suggestionsLimit: Int): SuggestionsInfo =
            getSentenceSuggestions(textInfo, suggestionsLimit).getSuggestionsInfoAt(0)

        @Deprecated("Android uses the sentence API starting with API 16")
        override fun onGetSuggestionsMultiple(
            textInfos: Array<out TextInfo>,
            suggestionsLimit: Int,
            sequentialWords: Boolean
        ): Array<SuggestionsInfo> = textInfos.map { textInfo ->
            onGetSuggestions(textInfo, suggestionsLimit).also {
                it.setCookieAndSequence(textInfo.cookie, textInfo.sequence)
            }
        }.toTypedArray()

        private fun getSentenceSuggestions(
            textInfo: TextInfo,
            suggestionsLimit: Int
        ): SentenceSuggestionsInfo {
            val text = textInfo.text.orEmpty()
            if (text.isBlank()) return emptySentence(textInfo)

            return try {
                val result = client.check(text, settingsRepository.load())
                SuggestionMapper.toSentenceSuggestions(result.matches, textInfo, suggestionsLimit)
            } catch (error: Exception) {
                RuntimeLog.error("Text check failed", error)
                emptySentence(textInfo)
            }
        }

        private fun emptySentence(textInfo: TextInfo): SentenceSuggestionsInfo =
            SuggestionMapper.emptySentence(textInfo)
    }
}
