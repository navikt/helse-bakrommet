package no.nav.helse.bakrommet.aareg

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
import org.slf4j.LoggerFactory

object AARegMock {
    private val log = LoggerFactory.getLogger(AARegMock::class.java)

    // Default test konfigurasjon
    val defaultConfiguration =
        Configuration.AAReg(
            hostname = "aareg-host",
            scope = OAuthScope("aareg-scope"),
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

    fun aaregMockHttpClient(
        configuration: Configuration.AAReg = defaultConfiguration,
        oboClient: OboClient = createDefaultOboClient(),
        fnrTilArbeidsforhold: Map<String, List<Arbeidsforhold>> = emptyMap(),
    ) = mockHttpClient { request ->
        val auth = request.headers[HttpHeaders.Authorization]!!
        if (auth != "Bearer ${configuration.scope.oboTokenFor(oboClient)}") {
            respondError(HttpStatusCode.Unauthorized)
        } else {
            log.info("URL: " + request.url)
            log.info("BODY: " + String(request.body.toByteArray()))
            log.info("PARAMS: " + request.url.parameters)
            log.info("HEADERS: " + request.headers)
            // assertNotNull(request.headers["Nav-Call-Id"])

            val fnr = request.headers["Nav-Personident"]!!

            if (fnr.endsWith("403")) {
                respond(
                    status = HttpStatusCode.Forbidden,
                    content = "403",
                )
            } else {
                val arbeidsforhold = fnrTilArbeidsforhold[fnr] ?: emptyList()
                val svar = objectMapper.writeValueAsString(arbeidsforhold)
                respond(
                    status = HttpStatusCode.OK,
                    content = svar,
                    headers = headersOf("Content-Type" to listOf("application/json")),
                )
            }
        }
    }

    fun aaRegClientMock(
        configuration: Configuration.AAReg = defaultConfiguration,
        oboClient: OboClient = createDefaultOboClient(),
        fnrTilArbeidsforhold: Map<String, List<Arbeidsforhold>> = emptyMap(),
    ) = AARegClient(
        configuration = configuration,
        oboClient = oboClient,
        httpClient = aaregMockHttpClient(configuration, oboClient, fnrTilArbeidsforhold),
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
