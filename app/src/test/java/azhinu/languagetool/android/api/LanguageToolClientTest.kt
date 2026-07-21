package azhinu.languagetool.android.api

import azhinu.languagetool.android.model.LanguageToolSettings
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LanguageToolClientTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun sendsPostToCheckEndpointAndParsesSuccess() {
        server.enqueue(MockResponse().setResponseCode(200).setBody(LanguageToolProtocolTest.RESPONSE))

        val result = LanguageToolClient().check("Helo world", settings())
        val request = server.takeRequest(1, TimeUnit.SECONDS)!!

        assertEquals("POST", request.method)
        assertEquals("/v2/check", request.path)
        assertTrue(request.body.readUtf8().contains("language=auto"))
        assertEquals(2, result.matches.size)
    }

    @Test
    fun exposesHttpStatusAndErrorBody() {
        server.enqueue(MockResponse().setResponseCode(429).setBody("Rate limit exceeded"))

        val error = runCatching { LanguageToolClient().check("Text", settings()) }.exceptionOrNull()

        assertTrue(error is LanguageToolHttpException)
        assertEquals(429, (error as LanguageToolHttpException).statusCode)
        assertTrue(error.responseBody.contains("Rate limit"))
    }

    @Test
    fun rejectsInvalidJsonFromSuccessfulResponse() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("Not JSON"))

        val error = runCatching { LanguageToolClient().check("Text", settings()) }.exceptionOrNull()

        assertTrue(error is IOException)
        assertTrue(error?.message.orEmpty().contains("JSON"))
    }

    @Test
    fun reportsReadTimeout() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(LanguageToolProtocolTest.RESPONSE)
                .setBodyDelay(300, TimeUnit.MILLISECONDS)
        )

        val error = runCatching {
            LanguageToolClient(connectTimeoutMs = 100, readTimeoutMs = 50).check("Text", settings())
        }.exceptionOrNull()

        assertTrue(error is IOException)
        assertTrue(error?.cause is SocketTimeoutException)
    }

    private fun settings() = LanguageToolSettings(endpoint = server.url("/").toString())
}
