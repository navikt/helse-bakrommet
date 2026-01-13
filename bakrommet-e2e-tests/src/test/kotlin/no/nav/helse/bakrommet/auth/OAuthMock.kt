package no.nav.helse.bakrommet.auth

import no.nav.helse.bakrommet.api.ApiModule
import no.nav.security.mock.oauth2.MockOAuth2Server

class OAuthMock {
    private val mockOAuth2Server: MockOAuth2Server =
        MockOAuth2Server().also {
            it.start()
        }
    private val issuerId = "EntraID"
    private val clientId = "bakrommet-dev"

    val authConfig =
        ApiModule.Configuration.Auth(
            clientId = clientId,
            issuerUrl = mockOAuth2Server.issuerUrl(issuerId).toString(),
            jwkProviderUri = mockOAuth2Server.jwksUrl(issuerId).toString(),
            tokenEndpoint = mockOAuth2Server.tokenEndpointUrl(issuerId).toString(),
        )

    fun token(
        audience: String = clientId,
        grupper: List<String> = listOf("GRUPPE_SAKSBEHANDLER"),
        navIdent: String = "tullebruker",
    ): String =
        mockOAuth2Server
            .issueToken(
                issuerId = issuerId,
                audience = audience,
                subject = "tullesubjekt",
                claims =
                    mapOf(
                        "NAVident" to navIdent,
                        "name" to "Tulla Bruker",
                        "groups" to grupper,
                        "preferred_username" to "tullabruker@nav.no",
                    ),
            ).serialize()
}
