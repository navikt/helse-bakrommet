package no.nav.helse.bakrommet.obo

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.jackson.JacksonConverter
import no.nav.helse.bakrommet.auth.AccessToken
import no.nav.helse.bakrommet.auth.OAuthScope
import no.nav.helse.bakrommet.auth.OboToken
import no.nav.helse.bakrommet.auth.TokenUtvekslingProvider
import no.nav.helse.bakrommet.sikkerLogger

class OboClient(
    private val configuration: OboModule.Configuration,
    private val oboClient: HttpClient =
        HttpClient(Apache) {
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter())
            }
        },
) : TokenUtvekslingProvider {
    override suspend fun exchangeToken(
        accessToken: AccessToken,
        scope: OAuthScope,
    ): OboToken {
        val oboTokenResponse =
            oboClient.post(configuration.url) {
                contentType(ContentType.Application.Json)
                setBody(
                    jacksonObjectMapper()
                        .createObjectNode()
                        .apply {
                            put("identity_provider", "azuread")
                            put("target", scope.asDefaultScope())
                            put("user_token", accessToken.value)
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
