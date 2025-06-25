package no.nav.helse.bakrommet.sigrun

import io.ktor.client.engine.mock.*
import io.ktor.http.*
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.TestOppsett.oboClient
import no.nav.helse.bakrommet.TestOppsett.oboTokenFor
import no.nav.helse.bakrommet.mockHttpClient
import no.nav.helse.bakrommet.util.toJsonNode
import org.junit.jupiter.api.assertNotNull

object SigrunMock {
    fun sigrunMockHttpClient(fnrÅrTilSvar: Map<Pair<String, String>, String> = mapOf()) =
        mockHttpClient { request ->
            val auth = request.headers[HttpHeaders.Authorization]!!
            if (auth != "Bearer ${TestOppsett.configuration.sigrun.scope.oboTokenFor()}") {
                respondError(HttpStatusCode.Unauthorized)
            } else {
                assertNotNull(request.headers["Nav-Call-Id"])

                val fnr = request.headers["Nav-Personident"]!!
                val inntektsAar = request.headers["inntektsaar"]!!

                val svar = fnrÅrTilSvar[fnr to inntektsAar]
                if (svar == null) {
                    respond(
                        status = HttpStatusCode.NotFound,
                        content = sigrunErrorResponse(status = 404, kode = "PGIF-008"),
                        headers = headersOf("Content-Type" to listOf("application/json")),
                    )
                } else {
                    val status = statusFromResp(svar) ?: 200
                    respond(
                        status = HttpStatusCode(status, "mock-$status"),
                        headers = headersOf("Content-Type" to listOf("application/json")),
                        content = svar,
                    )
                }
            }
        }

    fun sigrunMockClient(fnrÅrTilSvar: Map<Pair<String, String>, String> = mapOf()) =
        SigrunClient(
            configuration = TestOppsett.configuration.sigrun,
            oboClient = oboClient,
            httpClient = sigrunMockHttpClient(fnrÅrTilSvar),
        )

    private fun statusFromResp(resp: String): Int? {
        return try {
            resp.toJsonNode()["status"]?.asInt()
        } catch (ex: Exception) {
            return null
        }
    }

    fun sigrunErrorResponse(
        status: Int = 404,
        kode: String = "PGIF-008",
    ) = """
        {"timestamp":"2025-06-24T09:36:23.209+0200","status":$status,"error":"Not Found","source":"SKE",
        "message":"Fant ikke pensjonsgivende inntekt for oppgitt personidentifikator og inntektsår..  Korrelasjonsid: bb918c1c1cfa10a396e723949ae25f80. Spurt på år 2021 og tjeneste Pensjonsgivende Inntekt For Folketrygden",
        "path":"/api/v1/pensjonsgivendeinntektforfolketrygden",
        "ske-message":{"kode":"$kode-008","melding":"Fant ikke pensjonsgivende inntekt for oppgitt personidentifikator og inntektsår.","korrelasjonsid":"bb918c1c1cfa10a396e723949ae25f80"}}    
        """.trimIndent()
}
