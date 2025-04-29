package no.nav.helse.bakrommet.pdl

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import kotlinx.coroutines.runBlocking
import no.nav.helse.bakrommet.Configuration
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PdlClientTest {


    private fun mockHttpClient(requestHandler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
        HttpClient(MockEngine) {
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter())
            }
            engine {
                addHandler(requestHandler)
            }
        }

    val pdlReply = """
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

    val pdlUgyldigIdentReply = """
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

    val token = "PDL-TOKEN"
    val mockPdl = mockHttpClient { request ->
        val auth = request.headers[HttpHeaders.Authorization]!!
        if (auth != "Bearer $token") {
            respondError(HttpStatusCode.Unauthorized)
        } else {
            val json = jacksonObjectMapper().readValue(request.body.toByteArray(), JsonNode::class.java)
            val ident = json["variables"]["ident"].asText()
            if (ident == "1234") {
                respond(
                    status = HttpStatusCode.OK, content = pdlReply,
                    headers = headersOf("Content-Type" to listOf("application/json"))
                )
            } else if (ident == "error") {
                respond(
                    status = HttpStatusCode.OK, content = pdlUgyldigIdentReply,
                    headers = headersOf("Content-Type" to listOf("application/json"))
                )
            } else {
                respondError(HttpStatusCode.NotFound)
            }
        }
    }

    val pdl = PdlClient(
        configuration = Configuration.PDL(hostname = "host", scope = "scope"),
        httpClient = mockPdl
    )

    @Test
    fun `returnerer identer`() {
        val resp = runBlocking { pdl.hentIdenterFor(pdlToken = token, ident = "1234") }
        assertEquals(setOf("12345678910", "10987654321"), resp.toSet())
    }

    @Test
    fun `returnerer tom liste ved ukjent ident (404)`() {
        assertEquals(emptySet(),
            runBlocking { pdl.hentIdenterFor(pdlToken = token, ident = "5555") })
    }

    @Test
    fun `returnerer tom liste ved errors`() {
        assertEquals(emptySet(),
            runBlocking { pdl.hentIdenterFor(pdlToken = token, ident = "error") })
    }



}



