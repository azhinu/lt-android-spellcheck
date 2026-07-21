package azhinu.languagetool.android.dictionary

import android.content.Context
import android.provider.UserDictionary
import azhinu.languagetool.android.logging.RuntimeLog

class SystemDictionaryRepository(context: Context) {
    private val appContext = context.applicationContext

    fun loadWords(): Set<String> = runCatching {
        buildSet {
            appContext.contentResolver.query(
                UserDictionary.Words.CONTENT_URI,
                arrayOf(UserDictionary.Words.WORD),
                null,
                null,
                null
            )?.use { cursor ->
                val wordColumn = cursor.getColumnIndexOrThrow(UserDictionary.Words.WORD)
                while (cursor.moveToNext()) {
                    cursor.getString(wordColumn)?.trim()?.takeIf(String::isNotEmpty)?.let(::add)
                }
            }
        }
    }.onFailure {
        RuntimeLog.warn("System dictionary could not be read", it.message)
    }.getOrDefault(emptySet())

    fun addWords(words: Collection<String>) {
        words.map(String::trim).filter(String::isNotEmpty).forEach { word ->
            runCatching {
                UserDictionary.Words.addWord(appContext, word, 250, null, null)
            }.onFailure {
                RuntimeLog.warn("Word could not be added to the system dictionary", "$word: ${it.message}")
            }
        }
    }

    fun removeWords(words: Collection<String>) {
        words.forEach { word ->
            runCatching {
                appContext.contentResolver.delete(
                    UserDictionary.Words.CONTENT_URI,
                    "${UserDictionary.Words.WORD} = ? COLLATE NOCASE",
                    arrayOf(word)
                )
            }.onFailure {
                RuntimeLog.warn("Word could not be removed from the system dictionary", "$word: ${it.message}")
            }
        }
    }
}
