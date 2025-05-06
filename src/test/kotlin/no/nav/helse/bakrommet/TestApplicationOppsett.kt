package no.nav.helse.bakrommet

import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.helse.bakrommet.auth.OAuthMock
import no.nav.helse.bakrommet.auth.OboClient
import no.nav.helse.bakrommet.db.TestcontainersDatabase
import no.nav.helse.bakrommet.pdl.PdlClient
import org.junit.jupiter.api.Assertions.assertEquals

object TestOppsett {
    val oAuthMock = OAuthMock()

    val configuration =
        Configuration(
            TestcontainersDatabase.configuration,
            Configuration.OBO(url = "OBO-url"),
            Configuration.PDL(hostname = "PDL-hostname", scope = "PDL-scope"),
            oAuthMock.authConfig,
            Configuration.SykepengesoknadBackend(
                hostname = "sykepengesoknad-backend",
                scope = "sykepengesoknad-backend-scope",
            ),
            "test",
        )
    val oboToken = "OBO-TOKEN"
    val userToken = oAuthMock.token()

    val mockTexas =
        mockHttpClient { request ->
            if (request.bodyToJson()["user_token"].asText() != userToken) {
                respondError(HttpStatusCode.Unauthorized)
            } else {
                respond(
                    status = HttpStatusCode.OK,
                    content =
                        """
                        {"access_token": "$oboToken"}
                        """.trimIndent(),
                    headers = headersOf("Content-Type" to listOf("application/json")),
                )
            }
        }

    val mockPdl =
        mockHttpClient { request ->
            val auth = request.headers[HttpHeaders.Authorization]!!
            if (auth != "Bearer $oboToken") {
                respondError(HttpStatusCode.Unauthorized)
            } else {
                val json = request.bodyToJson()
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

    val oboClient = OboClient(configuration.obo, mockTexas)
}

fun runApplicationTest(
    config: Configuration = TestOppsett.configuration,
    pdlClient: PdlClient = TestOppsett.pdl,
    oboClient: OboClient = TestOppsett.oboClient,
    testBlock: suspend ApplicationTestBuilder.() -> Unit,
) = testApplication {
    personsokModule(config, pdlClient, oboClient)
    testBlock()
}

fun ApplicationTestBuilder.personsokModule(
    config: Configuration,
    pdlClient: PdlClient,
    oboClient: OboClient,
) {
    application {
        settOppKtor(
            instansierDatabase(config.db),
            config,
            pdlClient = pdlClient,
            oboClient = oboClient,
        )
    }
}
