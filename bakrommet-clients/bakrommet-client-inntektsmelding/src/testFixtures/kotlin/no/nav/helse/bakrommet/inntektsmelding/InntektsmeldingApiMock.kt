package no.nav.helse.bakrommet.inntektsmelding

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import no.nav.helse.bakrommet.auth.AccessToken
import no.nav.helse.bakrommet.auth.OAuthScope
import no.nav.helse.bakrommet.auth.OboToken
import no.nav.helse.bakrommet.auth.TokenUtvekslingProvider
import no.nav.helse.bakrommet.client.common.ApplicationConfig
import no.nav.helse.bakrommet.client.common.mockHttpClient
import no.nav.helse.bakrommet.objectMapper
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger

object InntektsmeldingApiMock {
    private val log = LoggerFactory.getLogger(InntektsmeldingApiMock::class.java)

    object Person1 {
        val fnr = "08088811111"
        val inntektsmeldinger = listOf(skapInntektsmelding(arbeidstakerFnr = fnr))
    }

    // Default test konfigurasjon
    val defaultConfiguration =
        InntektsmeldingClientModule.Configuration(
            baseUrl = "http://localhost",
            scope = OAuthScope("im-scope"),
            ApplicationConfig(
                podName = "unknownHost",
                appName = "unknownApp",
                imageName = "unknownImage",
            ),
        )

    // Default OBO client for testing
    fun createDefaultOboClient(): TokenUtvekslingProvider =
        object : TokenUtvekslingProvider {
            override suspend fun exchangeToken(
                accessToken: AccessToken,
                scope: OAuthScope,
            ): OboToken = OboToken("OBO-TOKEN_FOR_${scope.asDefaultScope()}")
        }

    fun inntektsmeldingMockHttpClient(
        configuration: InntektsmeldingClientModule.Configuration = defaultConfiguration,
        fnrTilInntektsmeldinger: Map<String, List<Inntektsmelding>> = mapOf(Person1.fnr to Person1.inntektsmeldinger),
        callCounter: AtomicInteger? = null,
    ): HttpClient {
        // Generer mapping fra inntektsmeldingId til Inntektsmelding for GET-forespørsler
        val inntektsmeldingIdTilInntektsmelding: Map<String, Inntektsmelding> =
            fnrTilInntektsmeldinger.values.flatten().associateBy { it.inntektsmeldingId }

        return mockHttpClient { request ->
            callCounter?.incrementAndGet()
            val auth = request.headers[HttpHeaders.Authorization]!!
            if (auth != "Bearer ${configuration.scope.oboTokenFor()}") {
                respondError(HttpStatusCode.Unauthorized)
            } else {
                log.info("URL: " + request.url)
                log.info("BODY: " + String(request.body.toByteArray()))
                log.info("PARAMS: " + request.url.parameters)
                log.info("HEADERS: " + request.headers)

                if (request.method == HttpMethod.Post) {
                    val payload = request.bodyToJson()
                    // assertNotNull(request.headers["Nav-Call-Id"])
                    val fnr = payload["fnr"].asText()
                    if (fnr.endsWith("400")) {
                        respond(
                            status = HttpStatusCode.BadRequest,
                            content = "400",
                        )
                    } else {
                        val inntektsmeldinger = fnrTilInntektsmeldinger[fnr]
                        if (inntektsmeldinger == null) {
                            respond(
                                status = HttpStatusCode.NotFound,
                                content = "404",
                            )
                        } else {
                            val svar = objectMapper.writeValueAsString(inntektsmeldinger)
                            respond(
                                status = HttpStatusCode.OK,
                                content = svar,
                                headers = headersOf("Content-Type" to listOf("application/json")),
                            )
                        }
                    }
                } else if (request.method == HttpMethod.Get) {
                    val inntektsmeldingId =
                        request.url
                            .toString()
                            .split('/')
                            .last()
                    val inntektsmelding = inntektsmeldingIdTilInntektsmelding[inntektsmeldingId]
                    if (inntektsmelding == null) {
                        respond(
                            status = HttpStatusCode.NotFound,
                            content = "404",
                        )
                    } else {
                        val svar = objectMapper.writeValueAsString(inntektsmelding)
                        respond(
                            status = HttpStatusCode.OK,
                            content = svar,
                            headers = headersOf("Content-Type" to listOf("application/json")),
                        )
                    }
                } else {
                    respond(status = HttpStatusCode.BadRequest, content = "400")
                }
            }
        }
    }

    fun inntektsmeldingClientMock(
        configuration: InntektsmeldingClientModule.Configuration = defaultConfiguration,
        tokenUtvekslingProvider: TokenUtvekslingProvider = createDefaultOboClient(),
        mockClient: HttpClient? = null,
        fnrTilInntektsmeldinger: Map<String, List<Inntektsmelding>> = mapOf(Person1.fnr to Person1.inntektsmeldinger),
    ) = InntektsmeldingClient(
        configuration = configuration,
        tokenUtvekslingProvider = tokenUtvekslingProvider,
        httpClient = mockClient ?: inntektsmeldingMockHttpClient(configuration, fnrTilInntektsmeldinger),
    )
}

// Extension function for å lage OBO token
fun OAuthScope.oboTokenFor(): String = "OBO-TOKEN_FOR_api://$baseValue/.default"

suspend fun HttpRequestData.bodyToJson(): JsonNode = jacksonObjectMapper().readValue(body.toByteArray(), JsonNode::class.java)
