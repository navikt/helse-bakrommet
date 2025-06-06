package no.nav.helse.bakrommet.pdl

import io.ktor.client.engine.mock.*
import io.ktor.http.*
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.bodyToJson
import no.nav.helse.bakrommet.mockHttpClient

object PdlMock {
    val mockPdl =
        mockHttpClient { request ->
            val auth = request.headers[HttpHeaders.Authorization]!!
            if (auth != "Bearer ${TestOppsett.oboTokenFor(TestOppsett.configuration.pdl.scope)}") {

                respondError(HttpStatusCode.Unauthorized)
            } else {
                val json = request.bodyToJson()
                val ident = json["variables"]["ident"].asText()
                if (ident == "1234" || ident == "01010199999") {
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

    private val pdlReply =
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
