package no.nav.helse.bakrommet.sykepengesoknad

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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
import no.nav.helse.flex.sykepengesoknad.kafka.SykepengesoknadDTO
import java.time.LocalDate
import java.util.*

object SykepengesoknadBackendMock {
    // Default test konfigurasjon
    val defaultConfiguration =
        SykepengesøknadBackendClientModule.Configuration(
            hostname = "sykepengesoknad-backend",
            scope = OAuthScope("sykepengesoknad-scope"),
            appConfig =
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

    fun sykepengesoknadMockHttpClient(
        configuration: SykepengesøknadBackendClientModule.Configuration = defaultConfiguration,
        fnrTilSoknader: Map<String, List<SykepengesoknadDTO>> = emptyMap(),
    ) = mockHttpClient { request ->
        val auth = request.headers[HttpHeaders.Authorization]!!
        if (auth != "Bearer ${configuration.scope.oboTokenFor()}") {
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
                                } catch (_: Exception) {
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
        configuration: SykepengesøknadBackendClientModule.Configuration = defaultConfiguration,
        tokenUtvekslingProvider: TokenUtvekslingProvider = createDefaultOboClient(),
        fnrTilSoknader: Map<String, List<SykepengesoknadDTO>> = emptyMap(),
    ) = SykepengesoknadBackendClient(
        configuration = configuration,
        tokenUtvekslingProvider = tokenUtvekslingProvider,
        httpClient = sykepengesoknadMockHttpClient(configuration, fnrTilSoknader),
    )
}

// Extension function for å lage OBO token
fun OAuthScope.oboTokenFor(): String = "OBO-TOKEN_FOR_api://$baseValue/.default"

suspend fun HttpRequestData.bodyToJson(): JsonNode = jacksonObjectMapper().readValue(body.toByteArray(), JsonNode::class.java)
