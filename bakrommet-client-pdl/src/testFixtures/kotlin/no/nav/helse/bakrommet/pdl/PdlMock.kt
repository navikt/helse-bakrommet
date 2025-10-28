package no.nav.helse.bakrommet.pdl

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import no.nav.helse.bakrommet.Configuration
import no.nav.helse.bakrommet.auth.OAuthScope
import no.nav.helse.bakrommet.auth.OboClient
import no.nav.helse.bakrommet.util.objectMapper

object PdlMock {
    // Default test konfigurasjon
    val defaultConfiguration =
        Configuration.PDL(
            hostname = "pdl-host",
            scope = OAuthScope("pdl-scope"),
        )

    // Default OBO client for testing
    fun createDefaultOboClient(): OboClient {
        val oboConfig = Configuration.OBO(url = "OBO-url")
        return OboClient(
            oboConfig,
            mockHttpClient { request ->
                respond(
                    status = HttpStatusCode.OK,
                    content = """{"access_token": "OBO-TOKEN_FOR_${request.bodyToJson()["target"].asText()}"}""",
                    headers = headersOf("Content-Type" to listOf("application/json")),
                )
            },
        )
    }

    fun mockPdl(
        configuration: Configuration.PDL = defaultConfiguration,
        oboClient: OboClient = createDefaultOboClient(),
        identTilReplyMap: Map<String, String> =
            mapOf(
                "1234" to pdlReply(),
                "01010199999" to pdlReply(),
            ),
    ) = mockHttpClient { request ->
        val auth = request.headers[HttpHeaders.Authorization]!!
        if (auth != "Bearer ${configuration.scope.oboTokenFor(oboClient)}") {
            respondError(HttpStatusCode.Unauthorized)
        } else {
            val json = request.bodyToJson()
            val ident = json["variables"]["ident"].asText()
            val reply = identTilReplyMap[ident]
            if (reply != null) {
                respond(
                    status = HttpStatusCode.OK,
                    content = reply,
                    headers = headersOf("Content-Type" to listOf("application/json")),
                )
            } else if (ident == "error") {
                respond(
                    status = HttpStatusCode.OK,
                    content = pdlUgyldigIdentReply,
                    headers = headersOf("Content-Type" to listOf("application/json")),
                )
            } else {
                respond(
                    status = HttpStatusCode.OK,
                    content = personIkkeFunnetReply,
                    headers = headersOf("Content-Type" to listOf("application/json")),
                )
            }
        }
    }

    fun pdlClient(
        configuration: Configuration.PDL = defaultConfiguration,
        oboClient: OboClient = createDefaultOboClient(),
        identTilReplyMap: Map<String, String> =
            mapOf(
                "1234" to pdlReply(),
                "01010199999" to pdlReply(),
            ),
    ) = PdlClient(
        configuration = configuration,
        oboClient = oboClient,
        httpClient = mockPdl(configuration, oboClient, identTilReplyMap),
    )

    fun pdlReply(
        fnr: String = "12345678910",
        aktorId: String = "10987654321",
    ) = """
        {
          "data": {
            "hentIdenter": {
              "identer": [
                {
                  "ident": "$fnr",
                  "gruppe": "FOLKEREGISTERIDENT"
                },
                {
                  "ident": "$aktorId",
                  "gruppe": "AKTORID"
                }
              ]
            }
          }
        }        
        """.trimIndent()

    private val pdlUgyldigIdentReply =
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

    private val personIkkeFunnetReply =
        """
        {"errors":[{"message":"Fant ikke person","locations":[{"line":2,"column":3}],"path":["hentIdenter"],"extensions":{"code":"not_found","classification":"ExecutionAborted"}}],"data":{"hentIdenter":null}}
        """.trimIndent()
}

// Extension function for å lage OBO token
fun OAuthScope.oboTokenFor(oboClient: OboClient): String = "OBO-TOKEN_FOR_api://$baseValue/.default"

// Helper functions for mock HTTP client
fun mockHttpClient(requestHandler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
    HttpClient(MockEngine) {
        install(ContentNegotiation) {
            register(ContentType.Application.Json, JacksonConverter(objectMapper))
        }
        engine {
            addHandler(requestHandler)
        }
    }

suspend fun HttpRequestData.bodyToJson(): JsonNode = jacksonObjectMapper().readValue(body.toByteArray(), JsonNode::class.java)
