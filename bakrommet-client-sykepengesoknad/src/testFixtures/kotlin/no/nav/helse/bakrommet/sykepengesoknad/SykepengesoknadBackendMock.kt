package no.nav.helse.bakrommet.sykepengesoknad

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

object SykepengesoknadBackendMock {
    private val log = LoggerFactory.getLogger(SykepengesoknadBackendMock::class.java)

    // Default test konfigurasjon
    val defaultConfiguration =
        Configuration.SykepengesoknadBackend(
            hostname = "sykepengesoknad-backend",
            scope = OAuthScope("sykepengesoknad-scope"),
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

    fun sykepengesoknadMockHttpClient(
        configuration: Configuration.SykepengesoknadBackend = defaultConfiguration,
        oboClient: OboClient = createDefaultOboClient(),
    ) = mockHttpClient { request ->
        val auth = request.headers[HttpHeaders.Authorization]!!
        if (auth != "Bearer ${configuration.scope.oboTokenFor(oboClient)}") {
            respondError(HttpStatusCode.Unauthorized)
        } else {
            respond(
                status = HttpStatusCode.OK,
                content = "[]",
                headers = headersOf("Content-Type" to listOf("application/json")),
            )
        }
    }

    fun sykepengesoknadMock(
        configuration: Configuration.SykepengesoknadBackend = defaultConfiguration,
        oboClient: OboClient = createDefaultOboClient(),
    ) = SykepengesoknadBackendClient(
        configuration = configuration,
        oboClient = oboClient,
        httpClient = sykepengesoknadMockHttpClient(configuration, oboClient),
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
