package no.nav.helse.bakrommet.aareg

import io.ktor.client.engine.mock.*
import io.ktor.http.*
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.mockHttpClient
import org.junit.jupiter.api.assertNotNull
import org.slf4j.LoggerFactory

object AARegMock {
    private val log = LoggerFactory.getLogger(AARegMock::class.java)

    object Person1 {
        val fnr = "08088811111"
        val respV2 = AARegMock::class.java.getResource("/aareg_v2_eksempel_respons.json")!!.readText().trim()
    }

    fun aaregMockHttpClient(fnrTilSvar: Map<String, String> = mapOf(Person1.fnr to Person1.respV2)) =
        mockHttpClient { request ->
            val auth = request.headers[HttpHeaders.Authorization]!!
            if (auth != "Bearer ${TestOppsett.oboTokenFor("aareg-scope")}") {
                respondError(HttpStatusCode.Unauthorized)
            } else {
                log.info("URL: " + request.url)
                log.info("BODY: " + String(request.body.toByteArray()))
                log.info("PARAMS: " + request.url.parameters)
                log.info("HEADERS: " + request.headers)
                assertNotNull(request.headers["Nav-Call-Id"])

                val fnr = request.headers["Nav-Personident"]!!

                if (fnr.endsWith("403")) {
                    respond(
                        status = HttpStatusCode.Forbidden,
                        content = "403",
                    )
                } else {
                    val svar = fnrTilSvar[fnr] ?: "[]"
                    respond(
                        status = HttpStatusCode.OK,
                        content = svar,
                        headers = headersOf("Content-Type" to listOf("application/json")),
                    )
                }
            }
        }

    fun aaRegClientMock() =
        AARegClient(
            configuration = TestOppsett.configuration.aareg,
            oboClient = TestOppsett.oboClient,
            httpClient = aaregMockHttpClient(),
        )
}
