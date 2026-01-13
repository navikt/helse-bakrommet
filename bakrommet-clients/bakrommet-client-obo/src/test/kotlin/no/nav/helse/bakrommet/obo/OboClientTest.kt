package no.nav.helse.bakrommet.obo

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import kotlinx.coroutines.runBlocking
import no.nav.helse.bakrommet.auth.AccessToken
import no.nav.helse.bakrommet.auth.OAuthScope
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

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
                                "access_token": "et-annet-token"
                            }
                            """.trimIndent(),
                        )
                    }
                }
            }

        val response =
            runBlocking {
                OboClient(OboModule.Configuration("url"), mockTexas).exchangeToken(AccessToken("et-token"), OAuthScope("et-scope"))
            }
        assertEquals("et-annet-token", response.value)
    }
}
