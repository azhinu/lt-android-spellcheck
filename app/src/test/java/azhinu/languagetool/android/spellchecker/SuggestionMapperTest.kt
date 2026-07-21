package azhinu.languagetool.android.spellchecker

import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import azhinu.languagetool.android.model.LanguageToolMatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SuggestionMapperTest {
    private val textInfo = TextInfo("Helo world", 42, 7)

    @Test
    fun mapsOffsetsLengthsCookiesAndSuggestionLimit() {
        val sentence = SuggestionMapper.toSentenceSuggestions(
            listOf(spelling(), grammar()), textInfo, suggestionsLimit = 1
        )

        assertEquals(2, sentence.suggestionsCount)
        assertEquals(0, sentence.getOffsetAt(0))
        assertEquals(4, sentence.getLengthAt(0))
        assertEquals(5, sentence.getOffsetAt(1))
        assertEquals(5, sentence.getLengthAt(1))
        val spelling = sentence.getSuggestionsInfoAt(0)
        assertEquals(1, spelling.suggestionsCount)
        assertEquals("Hello", spelling.getSuggestionAt(0))
        assertEquals(42, spelling.cookie)
        assertEquals(7, spelling.sequence)
    }

    @Test
    fun usesTypoAndGrammarFlags() {
        val sentence = SuggestionMapper.toSentenceSuggestions(
            listOf(spelling(), grammar()), textInfo, suggestionsLimit = 5
        )

        val typoFlags = sentence.getSuggestionsInfoAt(0).suggestionsAttributes
        val grammarFlags = sentence.getSuggestionsInfoAt(1).suggestionsAttributes
        assertTrue(typoFlags and SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO != 0)
        assertTrue(grammarFlags and SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_GRAMMAR_ERROR != 0)
        assertTrue(typoFlags and SuggestionsInfo.RESULT_ATTR_HAS_RECOMMENDED_SUGGESTIONS != 0)
    }

    @Test
    fun createsSafeEmptySentence() {
        val sentence = SuggestionMapper.toSentenceSuggestions(emptyList(), textInfo, 5)
        assertEquals(1, sentence.suggestionsCount)
        assertEquals(0, sentence.getSuggestionsInfoAt(0).suggestionsCount)
        assertEquals(0, sentence.getSuggestionsInfoAt(0).suggestionsAttributes)
    }

    private fun spelling() = LanguageToolMatch(
        message = "Spelling", shortMessage = "Spelling", offset = 0, length = 4,
        replacements = listOf("Hello", "Help"), ruleId = "SPELL", issueType = "misspelling",
        categoryId = "TYPOS"
    )

    private fun grammar() = LanguageToolMatch(
        message = "Grammar", shortMessage = "Grammar", offset = 5, length = 5,
        replacements = listOf("world!"), ruleId = "GRAMMAR", issueType = "grammar",
        categoryId = "GRAMMAR"
    )
}
