package no.nav.helse.bakrommet

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.testing.*
import no.nav.helse.bakrommet.db.TestcontainersDatabase
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AppTest {

    private val mockOAuth2Server: MockOAuth2Server = MockOAuth2Server().also {
        it.start()
    }
    private val issuerId = "EntraID"
    private val clientId = "bakrommet-dev"

    private val configuration = AuthConfiguration(
        clientId = clientId,
        issuerUrl = mockOAuth2Server.issuerUrl(issuerId).toString(),
        jwkProviderUri = mockOAuth2Server.jwksUrl(issuerId).toString(),
        tokenEndpoint = mockOAuth2Server.tokenEndpointUrl(issuerId).toString(),
    )

    private fun token(audience: String = clientId): String = mockOAuth2Server.issueToken(
        issuerId = issuerId,
        audience = audience,
        subject = "tullesubjekt",
        claims = mapOf(
            "NAVident" to "tullebruker",
        )
    ).serialize()

    @Test
    fun `starter appen`() =
        testApplication {
            application {
                settOppKtor(
                    instansierDatabase(TestcontainersDatabase().dbModuleConfiguration),
                    configuration
                )
            }
            val response = client.get("/antallBehandlinger") {
                bearerAuth(token())
            }
            assertEquals(200, response.status.value)
            assertEquals("0", response.bodyAsText())

            val response401 = client.get("/antallBehandlinger") {
                bearerAuth(token("FEIL-AUDIENCE"))
            }
            assertEquals(401, response401.status.value)

        }
}
