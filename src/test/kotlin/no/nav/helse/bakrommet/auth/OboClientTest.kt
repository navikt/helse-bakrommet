package no.nav.helse.bakrommet.auth

import io.ktor.client.engine.mock.*
import kotlinx.coroutines.runBlocking
import no.nav.helse.bakrommet.Configuration
import no.nav.helse.bakrommet.mockHttpClient
import org.junit.jupiter.api.Test
import kotlin.test.assertIs

class OboClientTest {
    @Test
    fun `veksler token`() {
        val mockTexas =
            mockHttpClient {
                respond(
                    """
                    {
                        "access_token": "token"
                    }
                    """.trimIndent(),
                )
            }
        val response =
            runBlocking {
                OboClient(Configuration.OBO("url"), mockTexas).exchangeToken("et-token", "et-scope")
            }

        assertIs<OboToken>(response)
    }
}
