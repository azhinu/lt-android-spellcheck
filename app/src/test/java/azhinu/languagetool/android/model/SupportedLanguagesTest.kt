package azhinu.languagetool.android.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SupportedLanguagesTest {
    @Test
    fun languageCodesAreUnique() {
        assertEquals(SupportedLanguages.all.size, SupportedLanguages.all.map { it.code }.toSet().size)
    }
}
