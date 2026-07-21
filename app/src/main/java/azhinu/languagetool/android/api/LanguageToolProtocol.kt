package azhinu.languagetool.android.api

import azhinu.languagetool.android.logging.RuntimeLog
import azhinu.languagetool.android.model.LanguageToolMatch
import azhinu.languagetool.android.model.LanguageToolResult
import azhinu.languagetool.android.model.LanguageToolSettings
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

object LanguageToolRequestBuilder {
    fun build(text: String, settings: LanguageToolSettings): String {
        val values = linkedMapOf(
            "text" to text,
            "language" to "auto",
            "level" to if (settings.pickyMode) "picky" else "default",
            "preferredLanguages" to settings.preferredLanguages.sorted().joinToString(","),
            "forcePreferredLanguages" to settings.forcePreferredLanguages.toString(),
            "preferredVariants" to settings.preferredVariants.toSortedMap().values.joinToString(",")
        )
        settings.motherTongue?.takeIf { it.isNotBlank() }?.let { values["motherTongue"] = it }
        settings.ignoredRuleIds.takeIf { it.isNotEmpty() }?.let {
            values["disabledRules"] = it.sorted().joinToString(",")
        }
        return values.entries.joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())
}

object LanguageToolResponseParser {
    fun parse(
        body: String,
        sourceText: String,
        settings: LanguageToolSettings
    ): LanguageToolResult {
        val root = JSONObject(body)
        val detectedLanguage = root.optJSONObject("language")
            ?.optJSONObject("detectedLanguage")?.optString("code")
            ?.takeIf { it.isNotBlank() }
        val sourceLower = sourceText.lowercase(Locale.ROOT)
        val dictionaryLower = settings.dictionary.mapTo(hashSetOf()) { it.lowercase(Locale.ROOT) }
        val matchesJson = root.optJSONArray("matches")
        val matches = buildList {
            if (matchesJson == null) return@buildList
            for (index in 0 until matchesJson.length()) {
                val item = matchesJson.getJSONObject(index)
                val rule = item.optJSONObject("rule") ?: JSONObject()
                val category = rule.optJSONObject("category") ?: JSONObject()
                val replacementsJson = item.optJSONArray("replacements")
                val replacements = buildList {
                    if (replacementsJson != null) {
                        for (replacementIndex in 0 until replacementsJson.length()) {
                            replacementsJson.getJSONObject(replacementIndex).optString("value")
                                .takeIf { it.isNotBlank() }?.let(::add)
                        }
                    }
                }
                val match = LanguageToolMatch(
                    message = item.optString("message"),
                    shortMessage = item.optString("shortMessage"),
                    offset = item.optInt("offset"),
                    length = item.optInt("length"),
                    replacements = replacements,
                    ruleId = rule.optString("id"),
                    issueType = rule.optString("issueType"),
                    categoryId = category.optString("id")
                )
                val end = (match.offset + match.length).coerceAtMost(sourceLower.length)
                val validRange = match.offset >= 0 && end >= match.offset && match.offset <= sourceLower.length
                val matchedWord = if (validRange) sourceLower.substring(match.offset, end) else ""
                if (!(match.isSpelling && matchedWord in dictionaryLower)) add(match)
            }
        }
        RuntimeLog.debug("Check completed: ${matches.size} matches found", detectedLanguage)
        return LanguageToolResult(detectedLanguage, matches)
    }
}
