package no.nav.helse.bakrommet.pdl

import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import no.nav.helse.bakrommet.Configuration
import no.nav.helse.bakrommet.auth.OboToken
import no.nav.helse.bakrommet.bodyToJson
import no.nav.helse.bakrommet.mockHttpClient
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PdlClientTest {
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

    val pdlUgyldigIdentReply =
        """
        {
          "errors": [
            {
              "message": "Ugyldig ident",
              "locations": [
                {
                  "line": 2,
                  "column": 3
                }
              ],
              "path": [
                "hentIdenter"
              ],
              "extensions": {
                "id": "ugyldig_ident",
                "code": "bad_request",
                "classification": "ValidationError"
              }
            }
          ],
          "data": {
            "hentIdenter": null
          }
        }        
        """.trimIndent()

    val token = OboToken("PDL-TOKEN")

    val mockPdl =
        mockHttpClient { request ->
            val auth = request.headers[HttpHeaders.Authorization]!!
            if (auth != token.somBearerHeader()) {
                respondError(HttpStatusCode.Unauthorized)
            } else {
                val json = request.bodyToJson()
                val ident = json["variables"]["ident"].asText()
                if (ident == "1234") {
                    respond(
                        status = HttpStatusCode.OK,
                        content = pdlReply,
                        headers = headersOf("Content-Type" to listOf("application/json")),
                    )
                } else if (ident == "error") {
                    respond(
                        status = HttpStatusCode.OK,
                        content = pdlUgyldigIdentReply,
                        headers = headersOf("Content-Type" to listOf("application/json")),
                    )
                } else {
                    respondError(HttpStatusCode.NotFound)
                }
            }
        }

    val pdl =
        PdlClient(
            configuration = Configuration.PDL(hostname = "host", scope = "scope"),
            httpClient = mockPdl,
        )

    @Test
    fun `returnerer identer`() {
        val resp = runBlocking { pdl.hentIdenterFor(pdlToken = token, ident = "1234") }
        assertEquals(setOf("12345678910", "10987654321"), resp.toSet())
    }

    @Test
    fun `returnerer tom liste ved ukjent ident (404)`() {
        assertEquals(
            emptySet(),
            runBlocking { pdl.hentIdenterFor(pdlToken = token, ident = "5555") },
        )
    }

    @Test
    fun `returnerer tom liste ved errors`() {
        assertEquals(
            emptySet(),
            runBlocking { pdl.hentIdenterFor(pdlToken = token, ident = "error") },
        )
    }
}
