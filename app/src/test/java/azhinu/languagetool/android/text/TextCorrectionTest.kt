package azhinu.languagetool.android.text

import azhinu.languagetool.android.model.LanguageToolMatch
import org.junit.Assert.assertEquals
import org.junit.Test

class TextCorrectionTest {
    @Test
    fun replacesOnlyMatchedRange() {
        assertEquals("These are a test", TextCorrection.applyReplacement("This are a test", match(0, 4), "These"))
    }

    @Test
    fun insertsPunctuationForZeroLengthMatch() {
        assertEquals("Hello, world", TextCorrection.applyReplacement("Hello world", match(5, 0), ","))
    }

    @Test
    fun invalidServerOffsetsDoNotCorruptText() {
        assertEquals("Hello", TextCorrection.applyReplacement("Hello", match(20, 2), "Nope"))
    }

    private fun match(offset: Int, length: Int) = LanguageToolMatch(
        message = "Test message",
        shortMessage = "Test",
        offset = offset,
        length = length,
        replacements = emptyList(),
        ruleId = "TEST_RULE",
        issueType = "grammar",
        categoryId = "GRAMMAR"
    )
}
