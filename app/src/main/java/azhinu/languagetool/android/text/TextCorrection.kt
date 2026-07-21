package azhinu.languagetool.android.text

import azhinu.languagetool.android.model.LanguageToolMatch

object TextCorrection {
    fun applyReplacement(
        source: String,
        match: LanguageToolMatch,
        replacement: String
    ): String {
        val start = match.offset
        val end = start + match.length
        if (start < 0 || end < start || end > source.length) return source
        return source.replaceRange(start, end, replacement)
    }
}
