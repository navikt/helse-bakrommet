package no.nav.helse.bakrommet.auth

import no.nav.helse.bakrommet.Configuration
import no.nav.security.mock.oauth2.MockOAuth2Server

class OAuthMock {
    private val mockOAuth2Server: MockOAuth2Server =
        MockOAuth2Server().also {
            it.start()
        }
    private val issuerId = "EntraID"
    private val clientId = "bakrommet-dev"

    val authConfig =
        Configuration.Auth(
            clientId = clientId,
            issuerUrl = mockOAuth2Server.issuerUrl(issuerId).toString(),
            jwkProviderUri = mockOAuth2Server.jwksUrl(issuerId).toString(),
            tokenEndpoint = mockOAuth2Server.tokenEndpointUrl(issuerId).toString(),
        )

    fun token(audience: String = clientId): String =
        mockOAuth2Server.issueToken(
            issuerId = issuerId,
            audience = audience,
            subject = "tullesubjekt",
            claims =
                mapOf(
                    "NAVident" to "tullebruker",
                ),
        ).serialize()
}
