package no.nav.helse.bakrommet.sykepengesoknad

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.*
import io.ktor.serialization.jackson.JacksonConverter
import no.nav.helse.bakrommet.auth.OAuthScope
import no.nav.helse.bakrommet.auth.TokenUtvekslingProvider
import no.nav.helse.bakrommet.sykepengesoknad.SykepengesoknadBackendMock.createDefaultOboClient
import no.nav.helse.bakrommet.util.objectMapper
import no.nav.helse.bakrommet.util.serialisertTilString
import org.slf4j.LoggerFactory

object SykepengesoknadMock {
    private val log = LoggerFactory.getLogger(SykepengesoknadMock::class.java)

    private fun defaultConfiguration() =
        SykepengesøknadBackendClientModule.Configuration(
            hostname = "sykepengesoknad-backend",
            scope = OAuthScope("sykepengesoknad-backend-scope"),
        )

    fun sykepengersoknadBackendClientMock(
        tokenUtvekslingProvider: TokenUtvekslingProvider = createDefaultOboClient(),
        configuration: SykepengesøknadBackendClientModule.Configuration = defaultConfiguration(),
        fnrTilSvar: Map<String, String> = emptyMap(),
        søknadIdTilSvar: Map<String, JsonNode> = emptyMap(),
    ) = SykepengesoknadBackendClient(
        configuration = configuration,
        tokenUtvekslingProvider = tokenUtvekslingProvider,
        httpClient =
            sykepengersoknadHttpMock(
                configuration = configuration,
                fnrTilSvar = fnrTilSvar,
                søknadIdTilSvar = søknadIdTilSvar,
            ),
    )

    fun sykepengersoknadHttpMock(
        configuration: SykepengesøknadBackendClientModule.Configuration = defaultConfiguration(),
        fnrTilSvar: Map<String, String> = emptyMap(),
        søknadIdTilSvar: Map<String, JsonNode> = emptyMap(),
    ) = mockHttpClient { request ->
        val auth = request.headers[HttpHeaders.Authorization]!!
        val expectedToken = "Bearer OBO-TOKEN_FOR_api://${configuration.scope.baseValue}/.default"
        if (auth != expectedToken) {
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
                val hentSøknadRequest =
                    jacksonObjectMapper().readValue(
                        request.body.toByteArray(),
                        SykepengesoknadBackendClient.Companion.HentSoknaderRequest::class.java,
                    )
                fnrTilSvar[hentSøknadRequest.fnr].returner()
            } else if (request.method == HttpMethod.Get) {
                val søknadId =
                    request.url
                        .toString()
                        .split("/")
                        .last()
                søknadIdTilSvar[søknadId]?.serialisertTilString().returner()
            } else {
                throw IllegalArgumentException("Uhåndtert metode: ${request.method}")
            }
        }
    }

    private fun mockHttpClient(requestHandler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
        HttpClient(MockEngine) {
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter(objectMapper))
            }
            engine {
                addHandler(requestHandler)
            }
        }
}
