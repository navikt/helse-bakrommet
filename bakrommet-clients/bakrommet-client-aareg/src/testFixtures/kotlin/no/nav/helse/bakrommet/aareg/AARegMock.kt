package no.nav.helse.bakrommet.aareg

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.JacksonConverter
import no.nav.helse.bakrommet.auth.OAuthScope
import no.nav.helse.bakrommet.auth.OboToken
import no.nav.helse.bakrommet.auth.TokenUtvekslingProvider
import no.nav.helse.bakrommet.util.objectMapper
import org.slf4j.LoggerFactory

object AARegMock {
    private val log = LoggerFactory.getLogger(AARegMock::class.java)

    // Default test konfigurasjon
    val defaultConfiguration =
        AAregModule.Configuration(
            hostname = "aareg-host",
            scope = OAuthScope("aareg-scope"),
        )

    // Default OBO client for testing
    fun createDefaultOboClient(): TokenUtvekslingProvider =
        object : TokenUtvekslingProvider {
            override suspend fun exchangeToken(
                bearerToken: String,
                scope: OAuthScope,
            ): OboToken = OboToken("OBO-TOKEN_FOR_${scope.asDefaultScope()}")
        }

    fun aaregMockHttpClient(
        configuration: AAregModule.Configuration = defaultConfiguration,
        fnrTilArbeidsforhold: Map<String, List<Arbeidsforhold>> = emptyMap(),
    ) = mockHttpClient { request ->
        val auth = request.headers[HttpHeaders.Authorization]!!
        if (auth != "Bearer ${configuration.scope.oboTokenFor()}") {
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
        configuration: AAregModule.Configuration = defaultConfiguration,
        tokenUtvekslingProvider: TokenUtvekslingProvider = createDefaultOboClient(),
        fnrTilArbeidsforhold: Map<String, List<Arbeidsforhold>> = emptyMap(),
    ) = AARegClient(
        configuration = configuration,
        tokenUtvekslingProvider = tokenUtvekslingProvider,
        httpClient = aaregMockHttpClient(configuration, fnrTilArbeidsforhold),
    )
}

// Extension function for å lage OBO token
fun OAuthScope.oboTokenFor(): String = "OBO-TOKEN_FOR_api://$baseValue/.default"

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
