package azhinu.languagetool.android.dictionary

import org.junit.Assert.assertEquals
import org.junit.Test

class DictionaryCodecTest {
    @Test
    fun importTrimsLinesDropsBlanksAndBomAndDeduplicates() {
        assertEquals(setOf("Alpha", "beta"), DictionaryCodec.decode("\uFEFF Alpha \n\nbeta\nAlpha\n"))
    }

    @Test
    fun exportIsSortedAndEndsWithNewline() {
        assertEquals("Alpha\nbeta\n", DictionaryCodec.encode(setOf("beta", "Alpha")))
    }

    @Test
    fun emptyExportIsEmpty() {
        assertEquals("", DictionaryCodec.encode(emptySet()))
    }
}
