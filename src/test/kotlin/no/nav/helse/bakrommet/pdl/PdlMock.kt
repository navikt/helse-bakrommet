package no.nav.helse.bakrommet.pdl

import io.ktor.client.engine.mock.*
import io.ktor.http.*
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.TestOppsett.oboTokenFor
import no.nav.helse.bakrommet.bodyToJson
import no.nav.helse.bakrommet.mockHttpClient

object PdlMock {
    val mockPdl = mockPdl()

    fun mockPdl(
        identTilReplyMap: Map<String, String> =
            mapOf(
                "1234" to pdlReply(),
                "01010199999" to pdlReply(),
            ),
    ) = mockHttpClient { request ->
        val auth = request.headers[HttpHeaders.Authorization]!!
        if (auth != "Bearer ${TestOppsett.configuration.pdl.scope.oboTokenFor()}") {
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

    val pdlClient =
        PdlClient(
            configuration = TestOppsett.configuration.pdl,
            oboClient = TestOppsett.oboClient,
            httpClient = mockPdl,
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
