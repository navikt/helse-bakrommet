package no.nav.helse.bakrommet.inntektsmelding

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
        Configuration.Inntektsmelding(
            baseUrl = "http://localhost",
            scope = OAuthScope("im-scope"),
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

    fun inntektsmeldingMockHttpClient(
        configuration: Configuration.Inntektsmelding = defaultConfiguration,
        oboClient: OboClient = createDefaultOboClient(),
        fnrTilInntektsmeldinger: Map<String, List<Inntektsmelding>> = mapOf(Person1.fnr to Person1.inntektsmeldinger),
        callCounter: AtomicInteger? = null,
    ): HttpClient {
        // Generer mapping fra inntektsmeldingId til Inntektsmelding for GET-forespørsler
        val inntektsmeldingIdTilInntektsmelding: Map<String, Inntektsmelding> =
            fnrTilInntektsmeldinger.values.flatten().associateBy { it.inntektsmeldingId }

        return mockHttpClient { request ->
            callCounter?.incrementAndGet()
            val auth = request.headers[HttpHeaders.Authorization]!!
            if (auth != "Bearer ${configuration.scope.oboTokenFor(oboClient)}") {
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
        configuration: Configuration.Inntektsmelding = defaultConfiguration,
        oboClient: OboClient = createDefaultOboClient(),
        mockClient: HttpClient? = null,
        fnrTilInntektsmeldinger: Map<String, List<Inntektsmelding>> = mapOf(Person1.fnr to Person1.inntektsmeldinger),
    ) = InntektsmeldingClient(
        configuration = configuration,
        oboClient = oboClient,
        httpClient = mockClient ?: inntektsmeldingMockHttpClient(configuration, oboClient, fnrTilInntektsmeldinger),
    )
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
