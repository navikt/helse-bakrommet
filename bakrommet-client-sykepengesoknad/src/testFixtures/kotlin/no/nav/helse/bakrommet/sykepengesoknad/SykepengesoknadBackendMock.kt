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
import no.nav.helse.flex.sykepengesoknad.kafka.SykepengesoknadDTO
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.UUID

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
        fnrTilSoknader: Map<String, List<SykepengesoknadDTO>> = emptyMap(),
    ) = mockHttpClient { request ->
        val auth = request.headers[HttpHeaders.Authorization]!!
        if (auth != "Bearer ${configuration.scope.oboTokenFor(oboClient)}") {
            respondError(HttpStatusCode.Unauthorized)
        } else {
            if (request.method == HttpMethod.Post) {
                // Hent søknader basert på fnr
                val requestBody = request.bodyToJson()
                val fnr = requestBody["fnr"]?.asText() ?: ""
                val fomParam = requestBody["fom"]?.asText()
                val fom = fomParam?.let { LocalDate.parse(it) } ?: LocalDate.now().minusYears(1)

                val soknader = fnrTilSoknader[fnr]?.filter { it.fom?.let { soknadFom -> soknadFom >= fom } == true } ?: emptyList()

                respond(
                    status = HttpStatusCode.OK,
                    content = objectMapper.writeValueAsString(soknader),
                    headers = headersOf("Content-Type" to listOf("application/json")),
                )
            } else if (request.method == HttpMethod.Get) {
                // Hent enkelt søknad basert på id
                val soknadId =
                    request.url
                        .toString()
                        .split("/")
                        .last()
                val soknad =
                    fnrTilSoknader.values.flatten().find { søknad ->
                        // Prøv først direkte matching
                        søknad.id == soknadId ||
                            // Prøv også å matche mot UUID-konvertert versjon av søknad-ID (for bakrommet demo)
                            (
                                try {
                                    val soknadIdAsUuid = UUID.nameUUIDFromBytes(søknad.id.toByteArray())
                                    soknadIdAsUuid.toString() == soknadId
                                } catch (e: Exception) {
                                    false
                                }
                            )
                    }
                if (soknad != null) {
                    respond(
                        status = HttpStatusCode.OK,
                        content = objectMapper.writeValueAsString(soknad),
                        headers = headersOf("Content-Type" to listOf("application/json")),
                    )
                } else {
                    respondError(HttpStatusCode.NotFound)
                }
            } else {
                respond(
                    status = HttpStatusCode.OK,
                    content = "[]",
                    headers = headersOf("Content-Type" to listOf("application/json")),
                )
            }
        }
    }

    fun sykepengesoknadMock(
        configuration: Configuration.SykepengesoknadBackend = defaultConfiguration,
        oboClient: OboClient = createDefaultOboClient(),
        fnrTilSoknader: Map<String, List<SykepengesoknadDTO>> = emptyMap(),
    ) = SykepengesoknadBackendClient(
        configuration = configuration,
        oboClient = oboClient,
        httpClient = sykepengesoknadMockHttpClient(configuration, oboClient, fnrTilSoknader),
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
