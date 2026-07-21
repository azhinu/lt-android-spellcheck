package azhinu.languagetool.android.api

import azhinu.languagetool.android.model.LanguageToolSettings
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageToolProtocolTest {
    @Test
    fun requestContainsLanguagePreferencesAndEncodesText() {
        val settings = LanguageToolSettings(
            motherTongue = "ru",
            preferredLanguages = setOf("ru", "en"),
            forcePreferredLanguages = true,
            preferredVariants = mapOf("en" to "en-GB"),
            pickyMode = true,
            ignoredRuleIds = setOf("RULE_B", "RULE_A")
        )

        val parameters = decode(LanguageToolRequestBuilder.build("Привет & hello", settings))

        assertEquals("Привет & hello", parameters["text"])
        assertEquals("auto", parameters["language"])
        assertEquals("picky", parameters["level"])
        assertEquals("en,ru", parameters["preferredLanguages"])
        assertEquals("true", parameters["forcePreferredLanguages"])
        assertEquals("en-GB", parameters["preferredVariants"])
        assertEquals("ru", parameters["motherTongue"])
        assertEquals("RULE_A,RULE_B", parameters["disabledRules"])
    }

    @Test
    fun requestOmitsOptionalParameters() {
        val parameters = decode(LanguageToolRequestBuilder.build("Text", LanguageToolSettings()))
        assertFalse("motherTongue" in parameters)
        assertFalse("disabledRules" in parameters)
        assertEquals("default", parameters["level"])
    }

    @Test
    fun parserKeepsOffsetsReplacementsAndKinds() {
        val result = LanguageToolResponseParser.parse(RESPONSE, "Helo world", LanguageToolSettings())

        assertEquals("en-US", result.detectedLanguage)
        assertEquals(2, result.matches.size)
        assertEquals(0, result.matches[0].offset)
        assertEquals(4, result.matches[0].length)
        assertEquals(listOf("Hello", "Help"), result.matches[0].replacements)
        assertTrue(result.matches[0].isSpelling)
        assertFalse(result.matches[1].isSpelling)
    }

    @Test
    fun dictionaryFiltersOnlySpellingForExactWordIgnoringCase() {
        val settings = LanguageToolSettings(dictionary = setOf("HELO"))
        val result = LanguageToolResponseParser.parse(RESPONSE, "Helo world", settings)

        assertEquals(1, result.matches.size)
        assertEquals("PUNCTUATION_PARAGRAPH_END", result.matches.single().ruleId)
    }

    @Test
    fun emptyResponseIsAccepted() {
        val result = LanguageToolResponseParser.parse("""{"matches":[]}""", "Text", LanguageToolSettings())
        assertEquals(null, result.detectedLanguage)
        assertTrue(result.matches.isEmpty())
    }

    @Test(expected = Exception::class)
    fun malformedJsonIsRejected() {
        LanguageToolResponseParser.parse("Definitely not JSON", "Text", LanguageToolSettings())
    }

    private fun decode(body: String): Map<String, String> = body.split("&").associate { part ->
        val pieces = part.split("=", limit = 2)
        URLDecoder.decode(pieces[0], StandardCharsets.UTF_8.name()) to
            URLDecoder.decode(pieces[1], StandardCharsets.UTF_8.name())
    }

    companion object {
        val RESPONSE = """
            {
              "language": {"detectedLanguage": {"code": "en-US"}},
              "matches": [
                {
                  "message": "Possible spelling mistake",
                  "shortMessage": "Spelling mistake",
                  "offset": 0,
                  "length": 4,
                  "replacements": [{"value":"Hello"},{"value":"Help"}],
                  "rule": {"id":"MORFOLOGIK_RULE_EN_US","issueType":"misspelling","category":{"id":"TYPOS"}}
                },
                {
                  "message": "Possible missing punctuation",
                  "shortMessage": "Missing punctuation",
                  "offset": 10,
                  "length": 1,
                  "replacements": [{"value":"."}],
                  "rule": {"id":"PUNCTUATION_PARAGRAPH_END","issueType":"typographical","category":{"id":"PUNCTUATION"}}
                }
              ]
            }
        """.trimIndent()
    }
}
