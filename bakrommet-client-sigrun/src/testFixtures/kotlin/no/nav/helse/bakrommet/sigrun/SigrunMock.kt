package no.nav.helse.bakrommet.sigrun

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import no.nav.helse.bakrommet.Configuration
import no.nav.helse.bakrommet.auth.OAuthScope
import no.nav.helse.bakrommet.auth.OboClient
import no.nav.helse.bakrommet.sigrun.SigrunMock.sigrunErrorResponse
import no.nav.helse.bakrommet.util.asJsonNode
import no.nav.helse.bakrommet.util.objectMapper
import java.time.Year

fun client2010to2050(fnr: String) = SigrunMock.sigrunMockClient(fnrÅrTilSvar = fnrÅrTilSvar2010to2050(fnr))

fun fnrÅrTilSvar2010to2050(fnr: String): Map<Pair<String, Year>, String> =
    mapOf(
        *(2010..2100)
            .map { år ->
                (fnr to Year.of(år)) to sigrunÅr(fnr, Year.of(år), næring = år * 100)
            }.toTypedArray(),
    )

fun clientMedManglendeÅr(
    fnr: String,
    vararg manglendeÅr: Year,
): SigrunClient {
    val dataSomManglerNoenÅr =
        fnrÅrTilSvar2010to2050(fnr).mapValues { (fnrÅr, data) ->
            if (fnrÅr.second in manglendeÅr) {
                sigrunErrorResponse(status = 404, kode = "PGIF-008")
            } else {
                data
            }
        }
    return SigrunMock.sigrunMockClient(fnrÅrTilSvar = dataSomManglerNoenÅr)
}

fun sigrunÅr(
    fnr: String = "10419045026",
    år: Year = Year.of(2022),
    næring: Int = 350000,
) = """
    {"norskPersonidentifikator":"$fnr","inntektsaar":"$år",
    "pensjonsgivendeInntekt":
        [
            {"skatteordning":"FASTLAND","datoForFastsetting":"2025-06-24T07:32:48.777Z",
            "pensjonsgivendeInntektAvLoennsinntekt":null,
            "pensjonsgivendeInntektAvLoennsinntektBarePensjonsdel":null,
            "pensjonsgivendeInntektAvNaeringsinntekt":"$næring",
            "pensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage":"10000"}
        ]
    }    
    """.trimIndent()

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
        fnrÅrTilSvar: Map<Pair<String, Year>, String> = mapOf(),
    ) = mockHttpClient { request ->
        val auth = request.headers[HttpHeaders.Authorization]!!
        if (auth != "Bearer ${configuration.scope.oboTokenFor()}") {
            respondError(HttpStatusCode.Unauthorized)
        } else {
            // assertNotNull(request.headers["Nav-Call-Id"])

            val fnr = request.headers["Nav-Personident"]!!
            val inntektsAar = Year.of(request.headers["inntektsaar"]!!.toInt())

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
        fnrÅrTilSvar: Map<Pair<String, Year>, String> = mapOf(),
    ) = SigrunClient(
        configuration = configuration,
        oboClient = oboClient,
        httpClient = sigrunMockHttpClient(configuration, fnrÅrTilSvar),
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
        "ske-message":{"kode":"$kode","melding":"Fant ikke pensjonsgivende inntekt for oppgitt personidentifikator og inntektsår.","korrelasjonsid":"bb918c1c1cfa10a396e723949ae25f80"}}    
        """.trimIndent()
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

suspend fun HttpRequestData.bodyToJson(): com.fasterxml.jackson.databind.JsonNode =
    objectMapper
        .readValue(body.toByteArray(), com.fasterxml.jackson.databind.JsonNode::class.java)
