package azhinu.languagetool.android.dictionary

object DictionaryCodec {
    fun decode(content: String): Set<String> = content
        .removePrefix("\uFEFF")
        .lineSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .toSet()

    fun encode(words: Set<String>): String = words
        .sortedWith(String.CASE_INSENSITIVE_ORDER)
        .joinToString(separator = "\n", postfix = if (words.isEmpty()) "" else "\n")
}
