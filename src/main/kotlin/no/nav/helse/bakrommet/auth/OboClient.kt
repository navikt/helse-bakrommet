package no.nav.helse.bakrommet.auth

import com.auth0.jwt.impl.JWTParser
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
        scope: String,
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
        val accessToken = jsonResponse["access_token"].asText()

        try {
            val claims = JWTParser().parsePayload(accessToken).claims
            sikkerLogger.info("OboClient: Utstedte token med claims={}", claims)
        } catch (ex: Exception) {
            sikkerLogger.warn("OboClient: Error parsing accessToken")
        }

        return OboToken(accessToken)
    }
}

class OboToken(private val value: String) {
    fun somBearerHeader() = "Bearer $value"
}
