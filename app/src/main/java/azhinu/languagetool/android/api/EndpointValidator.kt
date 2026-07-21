package azhinu.languagetool.android.api

import java.net.URI

object EndpointValidator {
    fun normalize(value: String): Result<String> = runCatching {
        val input = value.trim()
        require(input.isNotEmpty()) { "Endpoint cannot be empty" }
        require(input.length <= 2048) { "Endpoint is too long" }

        val uri = URI(input)
        val scheme = uri.scheme?.lowercase()
        require(scheme == "http" || scheme == "https") { "Only HTTP and HTTPS are allowed" }
        require(!uri.host.isNullOrBlank()) { "Endpoint does not contain a server name" }
        require(uri.userInfo == null) { "Endpoint credentials are not supported" }
        require(uri.query == null && uri.fragment == null) { "Endpoint cannot contain a query or fragment" }
        val path = uri.rawPath.orEmpty().trimEnd('/')
        require(!path.endsWith("/v2/check", ignoreCase = true)) {
            "Enter the server endpoint without the final /v2/check path"
        }

        val origin = URI(scheme, null, uri.host, uri.port, null, null, null).toASCIIString()
        URI("$origin$path").toASCIIString()
    }
}
