package no.nav.helse.bakrommet

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.helse.bakrommet.auth.OboClient
import no.nav.helse.bakrommet.db.TestcontainersDatabase
import no.nav.helse.bakrommet.pdl.PdlClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PersonsokTest {
    private val oAuthMock = OAuthMock()

    private val configuration =
        Configuration(
            TestcontainersDatabase.configuration,
            Configuration.OBO("OBO-url"),
            Configuration.PDL("PDL-hostname", "PDL-scope"),
            oAuthMock.authConfig,
        )

    val oboToken = "OBO-TOKEN"

    val mockTexas =
        mockHttpClient { request ->
            respond(
                status = HttpStatusCode.OK,
                content =
                    """
                    {"access_token": "$oboToken"}
                    """.trimIndent(),
                headers = headersOf("Content-Type" to listOf("application/json")),
            )
        }

    val mockPdl =
        mockHttpClient { request ->
            val auth = request.headers[HttpHeaders.Authorization]!!
            if (auth != "Bearer $oboToken") {
                respondError(HttpStatusCode.Unauthorized)
            } else {
                val json = jacksonObjectMapper().readValue(request.body.toByteArray(), JsonNode::class.java)
                val ident = json["variables"]["ident"].asText()
                assertEquals("01010199999", ident)

                val pdlReply =
                    """
                    {
                      "data": {
                        "hentIdenter": {
                          "identer": [
                            {
                              "ident": "12345678910",
                              "gruppe": "FOLKEREGISTERIDENT"
                            },
                            {
                              "ident": "10987654321",
                              "gruppe": "AKTORID"
                            }
                          ]
                        }
                      }
                    }        
                    """.trimIndent()

                respond(
                    status = HttpStatusCode.OK,
                    content = pdlReply,
                    headers = headersOf("Content-Type" to listOf("application/json")),
                )
            }
        }

    val pdl =
        PdlClient(
            configuration = Configuration.PDL(hostname = "host", scope = "scope"),
            httpClient = mockPdl,
        )

    private val oboClient = OboClient(configuration.obo, mockTexas)

    @Test
    fun `henter identer fra PDL`() =
        testApplication {
            application {
                settOppKtor(
                    instansierDatabase(configuration.db),
                    configuration,
                    pdlClient = pdl,
                    oboClient = oboClient,
                )
            }

            val response =
                client.post("/v1/personsok") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        { "fødselsnummer": "01010199999" }
                        """.trimIndent(),
                    )
                    bearerAuth(oAuthMock.token())
                }
            assertEquals(200, response.status.value)
            assertEquals("""[12345678910, 10987654321]""", response.headers["identer"])
            assertEquals("""{ "personId": "abc12" }""", response.bodyAsText())
        }

    @Test
    fun `funker ikke med feil token`() =
        testApplication {
            application {
                settOppKtor(
                    instansierDatabase(configuration.db),
                    configuration,
                    pdlClient = pdl,
                    oboClient = oboClient,
                )
            }

            val response =
                client.post("/v1/personsok") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        { "fødselsnummer": "01010199999" }
                        """.trimIndent(),
                    )
                    bearerAuth(oAuthMock.token("FEIL-AUDIENCE"))
                }
            assertEquals(401, response.status.value)
        }
}
