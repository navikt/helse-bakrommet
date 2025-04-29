package no.nav.helse.bakrommet

import io.ktor.client.request.*
import io.ktor.server.testing.*
import no.nav.helse.bakrommet.db.TestcontainersDatabase
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

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

class AppTest {
    private val oAuthMock = OAuthMock()

    private val configuration =
        Configuration(
            TestcontainersDatabase.configuration,
            Configuration.OBO(url = "OBO-url"),
            Configuration.PDL(hostname = "PDL-hostname", scope = "PDL-scope"),
            oAuthMock.authConfig,
        )

    @Test
    fun `starter appen`() =
        testApplication {
            application {
                settOppKtor(
                    instansierDatabase(configuration.db),
                    configuration,
                )
            }
            assertEquals(200, client.get("/isalive").status.value)
            assertEquals(200, client.get("/isready").status.value)
        }
}
