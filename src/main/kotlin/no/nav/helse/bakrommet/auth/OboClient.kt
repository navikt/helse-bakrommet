package no.nav.helse.bakrommet.auth

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import no.nav.helse.bakrommet.Configuration
import no.nav.helse.bakrommet.util.sikkerLogger

class OboClient(
    private val configuration: Configuration.OBO,
    private val oboClient: HttpClient =
        HttpClient(Apache) {
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter())
            }
        },
) {
    suspend fun exchangeToken(
        bearerToken: String,
        scope: OAuthScope,
    ): OboToken {
        val oboTokenResponse =
            oboClient.post(configuration.url) {
                contentType(ContentType.Application.Json)
                setBody(
                    jacksonObjectMapper().createObjectNode().apply {
                        put("identity_provider", "azuread")
                        put("target", "api://$scope/.default")
                        put("user_token", bearerToken)
                    }.toString(),
                )
            }
        if (!oboTokenResponse.status.isSuccess()) {
            sikkerLogger.warn(oboTokenResponse.bodyAsText())
        }

        val jsonResponse = jacksonObjectMapper().readTree(oboTokenResponse.bodyAsText())
        return OboToken(jsonResponse["access_token"].asText())
    }
}

class OboToken(private val value: String) {
    fun somBearerHeader() = "Bearer $value"
}
