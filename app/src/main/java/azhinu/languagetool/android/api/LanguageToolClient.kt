package azhinu.languagetool.android.api

import azhinu.languagetool.android.logging.RuntimeLog
import azhinu.languagetool.android.model.LanguageToolMatch
import azhinu.languagetool.android.model.LanguageToolResult
import azhinu.languagetool.android.model.LanguageToolSettings
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.StandardCharsets
import kotlin.system.measureTimeMillis

class LanguageToolClient(
    private val connectTimeoutMs: Int = CONNECT_TIMEOUT_MS,
    private val readTimeoutMs: Int = READ_TIMEOUT_MS
) {
    fun check(text: String, settings: LanguageToolSettings): LanguageToolResult {
        val endpoint = EndpointValidator.normalize(settings.endpoint).getOrThrow()
        val requestUrl = URI("$endpoint/v2/check").toURL()
        val body = LanguageToolRequestBuilder.build(text, settings)
        var responseCode = -1
        var responseBody = ""
        var connection: HttpURLConnection? = null

        val duration = try {
            measureTimeMillis {
                connection = (requestUrl.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = connectTimeoutMs
                    readTimeout = readTimeoutMs
                    instanceFollowRedirects = false
                    doOutput = true
                    setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "LanguageTool-Android/${azhinu.languagetool.android.BuildConfig.VERSION_NAME}")
                }
                connection!!.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
                responseCode = connection!!.responseCode
                val stream = if (responseCode in 200..299) connection!!.inputStream else connection!!.errorStream
                responseBody = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
            }
        } catch (error: Exception) {
            RuntimeLog.error("Could not connect to $endpoint", error)
            throw IOException("Could not connect to the server: ${error.message.orEmpty()}", error)
        } finally {
            connection?.disconnect()
        }

        RuntimeLog.info("Server responded with HTTP $responseCode in ${duration} ms", endpoint)
        if (responseCode !in 200..299) {
            val details = responseBody.take(1000).ifBlank { "Empty response" }
            RuntimeLog.warn("Server returned HTTP error $responseCode", details)
            throw LanguageToolHttpException(responseCode, details)
        }

        return try {
            LanguageToolResponseParser.parse(responseBody, text, settings)
        } catch (error: Exception) {
            RuntimeLog.error("Could not parse the server response", error)
            throw IOException("Server returned invalid JSON", error)
        }
    }

    fun test(settings: LanguageToolSettings): LanguageToolResult =
        check("This are a endpoint test.", settings)

    companion object {
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 20_000
    }
}

class LanguageToolHttpException(val statusCode: Int, val responseBody: String) :
    IOException("Server returned HTTP $statusCode: $responseBody")
