package no.nav.helse.bakrommet.pdl

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import no.nav.helse.bakrommet.auth.AccessToken
import no.nav.helse.bakrommet.auth.OAuthScope
import no.nav.helse.bakrommet.auth.OboToken
import no.nav.helse.bakrommet.auth.TokenUtvekslingProvider
import no.nav.helse.bakrommet.client.common.mockHttpClient

object PdlMock {
    // Default test konfigurasjon
    val defaultConfiguration =
        PdlClientModule.Configuration(
            hostname = "pdl-host",
            scope = OAuthScope("pdl-scope"),
        )

    // Default OBO client for testing
    fun createDefaultOboClient(): TokenUtvekslingProvider =
        object : TokenUtvekslingProvider {
            override suspend fun exchangeToken(
                accessToken: AccessToken,
                scope: OAuthScope,
            ): OboToken = OboToken("OBO-TOKEN_FOR_${scope.asDefaultScope()}")
        }

    fun mockPdl(
        configuration: PdlClientModule.Configuration = defaultConfiguration,
        identTilReplyMap: Map<String, String> =
            mapOf(
                "1234" to pdlReply(),
                "01010199999" to pdlReply(),
            ),
        pdlReplyGenerator: ((String) -> String?)? = null,
    ) = mockHttpClient { request ->
        val auth = request.headers[HttpHeaders.Authorization]!!
        if (auth != "Bearer ${configuration.scope.oboTokenFor()}") {
            respondError(HttpStatusCode.Unauthorized)
        } else {
            val json = request.bodyToJson()
            val ident = json["variables"]["ident"].asText()

            when {
                identTilReplyMap[ident] != null -> {
                    respond(
                        status = HttpStatusCode.OK,
                        content = identTilReplyMap[ident]!!,
                        headers = headersOf("Content-Type" to listOf("application/json")),
                    )
                }
                pdlReplyGenerator?.invoke(ident) != null -> {
                    val generatedReply = pdlReplyGenerator.invoke(ident)!!
                    respond(
                        status = HttpStatusCode.OK,
                        content = generatedReply,
                        headers = headersOf("Content-Type" to listOf("application/json")),
                    )
                }
                ident == "error" -> {
                    respond(
                        status = HttpStatusCode.OK,
                        content = pdlUgyldigIdentReply,
                        headers = headersOf("Content-Type" to listOf("application/json")),
                    )
                }
                else -> {
                    respond(
                        status = HttpStatusCode.OK,
                        content = personIkkeFunnetReply,
                        headers = headersOf("Content-Type" to listOf("application/json")),
                    )
                }
            }
        }
    }

    fun pdlClient(
        configuration: PdlClientModule.Configuration = defaultConfiguration,
        tokenUtvekslingProvider: TokenUtvekslingProvider = createDefaultOboClient(),
        identTilReplyMap: Map<String, String> =
            mapOf(
                "1234" to pdlReply(),
                "01010199999" to pdlReply(),
            ),
        pdlReplyGenerator: ((String) -> String?)? = null,
    ) = PdlClient(
        configuration = configuration,
        tokenUtvekslingProvider = tokenUtvekslingProvider,
        httpClient = mockPdl(configuration, identTilReplyMap, pdlReplyGenerator),
    )

    fun pdlReply(
        fnr: String = "01010199999",
        aktorId: String = "10987654321",
        fornavn: String = "Test",
        mellomnavn: String? = "Mellom",
        etternavn: String = "Testesen",
        foedselsdato: String = "1990-01-01",
    ): String {
        val mellomnavnJson = mellomnavn?.let { "\"$it\"" } ?: "null"
        return """
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
                },
                "hentPerson": {
                  "navn": [
                    {
                      "fornavn": "$fornavn",
                      "mellomnavn": $mellomnavnJson,
                      "etternavn": "$etternavn"
                    }
                  ],
                  "foedselsdato": [
                    {
                      "foedselsdato": "$foedselsdato"
                    }
                  ]
                }
              }
            }        
            """.trimIndent()
    }

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
fun OAuthScope.oboTokenFor(): String = "OBO-TOKEN_FOR_api://$baseValue/.default"

suspend fun HttpRequestData.bodyToJson(): JsonNode = jacksonObjectMapper().readValue(body.toByteArray(), JsonNode::class.java)
