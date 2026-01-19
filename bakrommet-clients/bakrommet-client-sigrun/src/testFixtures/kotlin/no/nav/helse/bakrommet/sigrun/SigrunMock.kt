package no.nav.helse.bakrommet.sigrun

import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import no.nav.helse.bakrommet.asJsonNode
import no.nav.helse.bakrommet.auth.AccessToken
import no.nav.helse.bakrommet.auth.OAuthScope
import no.nav.helse.bakrommet.auth.OboToken
import no.nav.helse.bakrommet.auth.TokenUtvekslingProvider
import no.nav.helse.bakrommet.client.common.ApplicationConfig
import no.nav.helse.bakrommet.client.common.mockHttpClient
import no.nav.helse.bakrommet.infrastruktur.provider.PensjonsgivendeInntektProvider
import no.nav.helse.bakrommet.sigrun.SigrunMock.sigrunErrorResponse
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
): PensjonsgivendeInntektProvider {
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
    lønnsinntekt: Int? = null,
) = """
    {"norskPersonidentifikator":"$fnr","inntektsaar":"$år",
    "pensjonsgivendeInntekt":
        [
            {"skatteordning":"FASTLAND","datoForFastsetting":"2025-06-24T07:32:48.777Z",
            "pensjonsgivendeInntektAvLoennsinntekt":"${lønnsinntekt?.toString() ?: "null"}",
            "pensjonsgivendeInntektAvLoennsinntektBarePensjonsdel":null,
            "pensjonsgivendeInntektAvNaeringsinntekt":"$næring",
            "pensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage": null}
        ]
    }    
    """.trimIndent()

object SigrunMock {
    // Default test konfigurasjon
    val defaultConfiguration =
        SigrunClientModule.Configuration(
            baseUrl = "http://localhost",
            scope = OAuthScope("sigrun-scope"),
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

    /**
     * Default handler for Sigrun-svar.
     * Dette brukes når ingen predefinert data finnes for en gitt fnr/år-kombinasjon.
     */
    fun sigrunDefaultReplyHandler(
        fnr: String,
        år: Year,
    ): String =
        when (år.value) {
            2024 -> {
                """
                {"norskPersonidentifikator":"$fnr","inntektsaar":2024,
                "pensjonsgivendeInntekt":
                    [
                        {"skatteordning":"FASTLAND","datoForFastsetting":"2025-06-24T07:31:26.035Z",
                        "pensjonsgivendeInntektAvLoennsinntekt":"250000",
                        "pensjonsgivendeInntektAvLoennsinntektBarePensjonsdel":null,
                        "pensjonsgivendeInntektAvNaeringsinntekt":null,
                        "pensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage":null}
                    ]
                }
                """.trimIndent()
            }

            2023 -> {
                """
                {"norskPersonidentifikator":"$fnr","inntektsaar":2023,
                "pensjonsgivendeInntekt":
                    [
                        {"skatteordning":"FASTLAND","datoForFastsetting":"2025-06-24T07:32:30.396Z",
                        "pensjonsgivendeInntektAvLoennsinntekt":"250000",
                        "pensjonsgivendeInntektAvLoennsinntektBarePensjonsdel":"",
                        "pensjonsgivendeInntektAvNaeringsinntekt":"25000",
                        "pensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage":null}
                    ]
                }
                """.trimIndent()
            }

            2022 -> {
                """
                {"norskPersonidentifikator":"$fnr","inntektsaar":2022,
                "pensjonsgivendeInntekt":
                    [
                        {"skatteordning":"FASTLAND","datoForFastsetting":"2025-06-24T07:32:48.777Z",
                        "pensjonsgivendeInntektAvLoennsinntekt":null,
                        "pensjonsgivendeInntektAvLoennsinntektBarePensjonsdel":null,
                        "pensjonsgivendeInntektAvNaeringsinntekt":"900000",
                        "pensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage":null}
                    ]
                }
                """.trimIndent()
            }

            2021 -> {
                """
                {"norskPersonidentifikator":"$fnr","inntektsaar":2021,
                "pensjonsgivendeInntekt":
                    [
                        {"skatteordning":"FASTLAND","datoForFastsetting":"2025-06-24T07:33:10.123Z",
                        "pensjonsgivendeInntektAvLoennsinntekt":"1000000",
                        "pensjonsgivendeInntektAvLoennsinntektBarePensjonsdel":null,
                        "pensjonsgivendeInntektAvNaeringsinntekt":"200",
                        "pensjonsgivendeInntektAvNaeringsinntektFraFiskeFangstEllerFamiliebarnehage":null}
                    ]
                }
                """.trimIndent()
            }

            2020 -> {
                """
                {"norskPersonidentifikator":"$fnr","inntektsaar":2020,
                "pensjonsgivendeInntekt":null}
                """.trimIndent()
            }

            else -> {
                // For andre år, returner 404
                sigrunErrorResponse(status = 404, kode = "PGIF-008")
            }
        }

    fun sigrunMockHttpClient(
        configuration: SigrunClientModule.Configuration = defaultConfiguration,
        fnrÅrTilSvar: Map<Pair<String, Year>, String> = mapOf(),
        defaultSigrunReplyHandler: (String, Year) -> String = { fnr, år -> sigrunDefaultReplyHandler(fnr, år) },
    ) = mockHttpClient { request ->
        val auth = request.headers[HttpHeaders.Authorization]!!
        if (auth != "Bearer ${configuration.scope.oboTokenFor()}") {
            respondError(HttpStatusCode.Unauthorized)
        } else {
            // assertNotNull(request.headers["Nav-Call-Id"])

            val fnr = request.headers["Nav-Personident"]!!
            val inntektsAar = Year.of(request.headers["inntektsaar"]!!.toInt())

            val svar = fnrÅrTilSvar[fnr to inntektsAar]
            val finalSvar = svar ?: defaultSigrunReplyHandler(fnr, inntektsAar)

            // Sjekk om svaret er en feilmelding (starter med "{" og inneholder "status")
            val isError =
                try {
                    finalSvar.asJsonNode()["status"] != null
                } catch (_: Exception) {
                    false
                }

            if (isError) {
                val status = statusFromResp(finalSvar) ?: 404
                respond(
                    status = HttpStatusCode(status, "mock-$status"),
                    content = finalSvar,
                    headers = headersOf("Content-Type" to listOf("application/json")),
                )
            } else {
                respond(
                    status = HttpStatusCode.OK,
                    headers = headersOf("Content-Type" to listOf("application/json")),
                    content = finalSvar,
                )
            }
        }
    }

    fun sigrunMockClient(
        configuration: SigrunClientModule.Configuration = defaultConfiguration,
        tokenUtvekslingProvider: TokenUtvekslingProvider = createDefaultOboClient(),
        fnrÅrTilSvar: Map<Pair<String, Year>, String> = mapOf(),
        defaultSigrunReplyHandler: (String, Year) -> String = { fnr, år -> sigrunDefaultReplyHandler(fnr, år) },
    ) = SigrunClient(
        configuration = configuration,
        tokenUtvekslingProvider = tokenUtvekslingProvider,
        httpClient = sigrunMockHttpClient(configuration, fnrÅrTilSvar, defaultSigrunReplyHandler),
    )

    private fun statusFromResp(resp: String): Int? {
        return try {
            resp.asJsonNode()["status"]?.asInt()
        } catch (_: Exception) {
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
