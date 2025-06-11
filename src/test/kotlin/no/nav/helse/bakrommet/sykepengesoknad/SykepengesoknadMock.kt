package no.nav.helse.bakrommet.sykepengesoknad

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.TestOppsett.oboClient
import no.nav.helse.bakrommet.TestOppsett.oboTokenFor
import no.nav.helse.bakrommet.mockHttpClient
import org.slf4j.LoggerFactory

object SykepengesoknadMock {
    private val log = LoggerFactory.getLogger(SykepengesoknadMock::class.java)

    fun sykepengersoknadBackendClientMock(
        fnrTilSvar: Map<String, String> = emptyMap(),
        søknadIdTilSvar: Map<String, String> = emptyMap(),
    ) = SykepengesoknadBackendClient(
        configuration = TestOppsett.configuration.sykepengesoknadBackend,
        oboClient = oboClient,
        httpClient = sykepengersoknadHttpMock(fnrTilSvar = fnrTilSvar, søknadIdTilSvar = søknadIdTilSvar),
    )

    fun sykepengersoknadHttpMock(
        fnrTilSvar: Map<String, String> = emptyMap(),
        søknadIdTilSvar: Map<String, String> = emptyMap(),
    ) = mockHttpClient { request ->
        val auth = request.headers[HttpHeaders.Authorization]!!
        if (auth != "Bearer ${TestOppsett.configuration.sykepengesoknadBackend.scope.oboTokenFor()}") {
            respondError(HttpStatusCode.Unauthorized)
        } else {
            log.info("URL: " + request.url)
            log.info("METHOD: " + request.method)
            log.info("BODY: " + String(request.body.toByteArray()))
            log.info("PARAMS: " + request.url.parameters)
            log.info("HEADERS: " + request.headers)

            fun String?.returner() =
                if (this == null) {
                    respond(
                        status = HttpStatusCode.NotFound,
                        content = "",
                    )
                } else {
                    respond(
                        status = HttpStatusCode.OK,
                        content = this,
                        headers = headersOf("Content-Type" to listOf("application/json")),
                    )
                }

            if (request.method == HttpMethod.Post) {
                val request =
                    jacksonObjectMapper().readValue(
                        request.body.toByteArray(),
                        SykepengesoknadBackendClient.Companion.HentSoknaderRequest::class.java,
                    )
                fnrTilSvar[request.fnr].returner()
            } else if (request.method == HttpMethod.Get) {
                val søknadId = request.url.toString().split("/").last()
                søknadIdTilSvar[søknadId].returner()
            } else {
                throw IllegalArgumentException("Uhåndtert metode: ${request.method}")
            }
        }
    }
}
