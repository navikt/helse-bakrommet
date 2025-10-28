package no.nav.helse.bakrommet.sigrun

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import no.nav.helse.bakrommet.Configuration
import no.nav.helse.bakrommet.auth.OAuthScope
import no.nav.helse.bakrommet.auth.OboClient
import no.nav.helse.bakrommet.util.asJsonNode
import no.nav.helse.bakrommet.util.objectMapper
// import org.junit.jupiter.api.assertNotNull

object SigrunMock {
    // Default test konfigurasjon
    val defaultConfiguration =
        Configuration.Sigrun(
            baseUrl = "http://localhost",
            scope = OAuthScope("sigrun-scope"),
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

    fun sigrunMockHttpClient(
        configuration: Configuration.Sigrun = defaultConfiguration,
        oboClient: OboClient = createDefaultOboClient(),
        fnrÅrTilSvar: Map<Pair<String, String>, String> = mapOf(),
    ) = mockHttpClient { request ->
        val auth = request.headers[HttpHeaders.Authorization]!!
        if (auth != "Bearer ${configuration.scope.oboTokenFor(oboClient)}") {
            respondError(HttpStatusCode.Unauthorized)
        } else {
            // assertNotNull(request.headers["Nav-Call-Id"])

            val fnr = request.headers["Nav-Personident"]!!
            val inntektsAar = request.headers["inntektsaar"]!!

            val svar = fnrÅrTilSvar[fnr to inntektsAar]
            if (svar == null) {
                respond(
                    status = HttpStatusCode.NotFound,
                    content = sigrunErrorResponse(status = 404, kode = "PGIF-008"),
                    headers = headersOf("Content-Type" to listOf("application/json")),
                )
            } else {
                val status = statusFromResp(svar) ?: 200
                respond(
                    status = HttpStatusCode(status, "mock-$status"),
                    headers = headersOf("Content-Type" to listOf("application/json")),
                    content = svar,
                )
            }
        }
    }

    fun sigrunMockClient(
        configuration: Configuration.Sigrun = defaultConfiguration,
        oboClient: OboClient = createDefaultOboClient(),
        fnrÅrTilSvar: Map<Pair<String, String>, String> = mapOf(),
    ) = SigrunClient(
        configuration = configuration,
        oboClient = oboClient,
        httpClient = sigrunMockHttpClient(configuration, oboClient, fnrÅrTilSvar),
    )

    private fun statusFromResp(resp: String): Int? {
        return try {
            resp.asJsonNode()["status"]?.asInt()
        } catch (ex: Exception) {
            return null
        }
    }

    fun sigrunErrorResponse(
        status: Int = 404,
        kode: String = "PGIF-008",
    ) = """
        {"timestamp":"2025-06-24T09:36:23.209+0200","status":$status,"error":"Not Found","source":"SKE",
        "message":"Fant ikke pensjonsgivende inntekt for oppgitt personidentifikator og inntektsår..  Korrelasjonsid: bb918c1c1cfa10a396e723949ae25f80. Spurt på år 2021 og tjeneste Pensjonsgivende Inntekt For Folketrygden",
        "path":"/api/v1/pensjonsgivendeinntektforfolketrygden",
        "ske-message":{"kode":"$kode-008","melding":"Fant ikke pensjonsgivende inntekt for oppgitt personidentifikator og inntektsår.","korrelasjonsid":"bb918c1c1cfa10a396e723949ae25f80"}}    
        """.trimIndent()
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

suspend fun HttpRequestData.bodyToJson(): com.fasterxml.jackson.databind.JsonNode =
    no.nav.helse.bakrommet.util.objectMapper
        .readValue(body.toByteArray(), com.fasterxml.jackson.databind.JsonNode::class.java)
