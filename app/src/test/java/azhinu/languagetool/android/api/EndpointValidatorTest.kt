package azhinu.languagetool.android.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EndpointValidatorTest {
    @Test
    fun normalizesBaseUrl() {
        assertEquals(
            "https://server.example",
            EndpointValidator.normalize(" https://server.example/ ").getOrThrow()
        )
    }

    @Test
    fun preservesPort() {
        assertEquals(
            "http://192.168.1.10:8081",
            EndpointValidator.normalize("http://192.168.1.10:8081").getOrThrow()
        )
    }

    @Test
    fun preservesSecretPathAndRemovesTrailingSlash() {
        assertEquals(
            "https://server.example/private-route",
            EndpointValidator.normalize("https://server.example/private-route/").getOrThrow()
        )
    }

    @Test
    fun preservesEncodedPathSegments() {
        assertEquals(
            "https://server.example/private%2Froute",
            EndpointValidator.normalize("https://server.example/private%2Froute/").getOrThrow()
        )
    }

    @Test
    fun rejectsCheckPath() {
        assertTrue(EndpointValidator.normalize("https://server.example/v2/check").isFailure)
        assertTrue(EndpointValidator.normalize("https://server.example/private/v2/check/").isFailure)
    }

    @Test
    fun rejectsUnsupportedScheme() {
        assertTrue(EndpointValidator.normalize("ftp://server.example").isFailure)
    }

    @Test
    fun rejectsCredentialsAndQuery() {
        assertTrue(EndpointValidator.normalize("https://user:pass@server.example").isFailure)
        assertTrue(EndpointValidator.normalize("https://server.example?token=nope").isFailure)
    }
}
