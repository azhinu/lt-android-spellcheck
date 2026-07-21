package azhinu.languagetool.android.spellchecker

import android.os.Build
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import azhinu.languagetool.android.model.LanguageToolMatch

object SuggestionMapper {
    val supportedAttributes: Int =
        SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO or
            SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_GRAMMAR_ERROR or
            SuggestionsInfo.RESULT_ATTR_HAS_RECOMMENDED_SUGGESTIONS

    fun toSentenceSuggestions(
        matches: List<LanguageToolMatch>,
        textInfo: TextInfo,
        suggestionsLimit: Int
    ): SentenceSuggestionsInfo {
        if (matches.isEmpty()) return emptySentence(textInfo)
        val infos = matches.map { match ->
            match.toSuggestionsInfo(textInfo, suggestionsLimit)
        }.toTypedArray()
        return SentenceSuggestionsInfo(
            infos,
            matches.map(LanguageToolMatch::offset).toIntArray(),
            matches.map(LanguageToolMatch::length).toIntArray()
        )
    }

    fun emptySentence(textInfo: TextInfo): SentenceSuggestionsInfo =
        SentenceSuggestionsInfo(
            arrayOf(SuggestionsInfo(0, emptyArray(), textInfo.cookie, textInfo.sequence)),
            intArrayOf(0),
            intArrayOf(0)
        )

    private fun LanguageToolMatch.toSuggestionsInfo(
        textInfo: TextInfo,
        suggestionsLimit: Int
    ): SuggestionsInfo {
        val limitedReplacements = replacements.take(suggestionsLimit.coerceAtLeast(0)).toTypedArray()
        var attributes = if (isSpelling || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO
        } else {
            SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_GRAMMAR_ERROR
        }
        if (limitedReplacements.isNotEmpty()) {
            attributes = attributes or SuggestionsInfo.RESULT_ATTR_HAS_RECOMMENDED_SUGGESTIONS
        }
        return SuggestionsInfo(attributes, limitedReplacements, textInfo.cookie, textInfo.sequence)
    }
}
