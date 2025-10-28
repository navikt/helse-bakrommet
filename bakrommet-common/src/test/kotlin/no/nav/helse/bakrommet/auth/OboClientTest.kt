package no.nav.helse.bakrommet.auth

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import kotlinx.coroutines.runBlocking
import no.nav.helse.bakrommet.Configuration
import org.junit.jupiter.api.Test
import kotlin.test.assertIs

class OboClientTest {
    @Test
    fun `veksler token`() {
        val mockTexas =
            HttpClient(MockEngine) {
                install(ContentNegotiation) {
                    register(ContentType.Application.Json, JacksonConverter())
                }
                engine {
                    addHandler {
                        respond(
                            """
                            {
                                "access_token": "token"
                            }
                            """.trimIndent(),
                        )
                    }
                }
            }

        val response =
            runBlocking {
                OboClient(Configuration.OBO("url"), mockTexas).exchangeToken("et-token", OAuthScope("et-scope"))
            }

        assertIs<OboToken>(response)
    }
}
