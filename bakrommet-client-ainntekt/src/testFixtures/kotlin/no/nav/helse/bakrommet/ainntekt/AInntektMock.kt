package no.nav.helse.bakrommet.ainntekt

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
import no.nav.helse.bakrommet.ereg.Organisasjon
import no.nav.helse.bakrommet.util.objectMapper
import no.nav.helse.bakrommet.util.serialisertTilString
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.YearMonth

object AInntektMock {
    private val log = LoggerFactory.getLogger(AInntektMock::class.java)

    // Default test konfigurasjon
    val defaultConfiguration =
        Configuration.AInntekt(
            hostname = "inntektskomponenten-host",
            scope = OAuthScope("inntektskomponenten-scope"),
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

    fun ainntektMockHttpClient(
        configuration: Configuration.AInntekt = defaultConfiguration,
        oboClient: OboClient = createDefaultOboClient(),
        fnrTilInntektApiUt: Map<String, InntektApiUt>,
    ) = mockHttpClient { request ->
        val auth = request.headers[HttpHeaders.Authorization]!!
        if (auth != "Bearer ${configuration.scope.oboTokenFor(oboClient)}") {
            respondError(HttpStatusCode.Unauthorized)
        } else {
            log.info("URL: " + request.url)
            log.info("BODY: " + String(request.body.toByteArray()))
            log.info("PARAMS: " + request.url.parameters)
            log.info("HEADERS: " + request.headers)

            val payload = request.bodyToJson()
            // assertNotNull(request.headers["Nav-Call-Id"])

            val fnr = payload["personident"].asText()
            val maanedFom = YearMonth.parse(payload["maanedFom"].asText())
            val maanedTom = YearMonth.parse(payload["maanedTom"].asText())

            if (fnr.endsWith("403")) {
                respond(
                    status = HttpStatusCode.Forbidden,
                    content = "403",
                )
            } else {
                val inntektApiUt = (fnrTilInntektApiUt[fnr] ?: InntektApiUt(data = emptyList())).filtrerMaaneder(maanedFom, maanedTom)

                respond(
                    status = HttpStatusCode.OK,
                    content = inntektApiUt.serialisertTilString(),
                    headers = headersOf("Content-Type" to listOf("application/json")),
                )
            }
        }
    }

    fun aInntektClientMock(
        configuration: Configuration.AInntekt = defaultConfiguration,
        oboClient: OboClient = createDefaultOboClient(),
        fnrTilInntektApiUt: Map<String, InntektApiUt>,
        mockClient: HttpClient? = null,
    ) = AInntektClient(
        configuration = configuration,
        oboClient = oboClient,
        httpClient = mockClient ?: ainntektMockHttpClient(configuration, oboClient, fnrTilInntektApiUt),
    )
}

fun etInntektSvar(
    virksomhet: String = "999999999",
    opplysningspliktig: String = "888888888",
    skjæringstidspunkt: YearMonth? = null,
    beloep: Int = 12000,
): InntektApiUt {
    // Hvis skjæringstidspunkt er gitt, generer måneder basert på det (3 måneder før skjæringstidspunkt)
    // Ellers bruk hardkodede måneder for bakoverkompatibilitet
    val maaneder =
        if (skjæringstidspunkt != null) {
            listOf(
                skjæringstidspunkt.minusMonths(3),
                skjæringstidspunkt.minusMonths(2),
                skjæringstidspunkt.minusMonths(1),
            )
        } else {
            listOf(
                YearMonth.parse("2022-08"),
                YearMonth.parse("2022-09"),
                YearMonth.parse("2022-10"),
                YearMonth.parse("2022-11"),
            )
        }

    return InntektApiUt(
        data =
            maaneder.map { maaned ->
                Inntektsinformasjon(
                    maaned = maaned,
                    underenhet = virksomhet,
                    opplysningspliktig = opplysningspliktig,
                    inntektListe =
                        listOf(
                            Inntekt(
                                type = "LOENNSINNTEKT",
                                beloep = java.math.BigDecimal.valueOf(beloep.toLong()),
                            ),
                        ),
                )
            },
    )
}

/**
 * Builder-funksjon for å generere ainntektsdata for en gitt periode.
 *
 * @param beloep Månedlig inntekt
 * @param fraMaaned Startmåned for inntektsperioden
 * @param virksomhetsnummer Organisasjonsnummer
 * @param antallMaanederTilbake Antall måneder tilbake fra fraMaaned (inkluderer fraMaaned)
 * @param inntektType Type inntekt (default: "LOENNSINNTEKT")
 * @param opplysningspliktig Opplysningspliktig organisasjonsnummer (default: samme som virksomhetsnummer)
 * @return Liste med Inntektsinformasjon for hver måned
 */
fun genererAinntektsdata(
    beloep: BigDecimal,
    fraMaaned: YearMonth,
    organisasjon: Organisasjon? = null,
    virksomhetsnummer: String = organisasjon?.orgnummer ?: "999888777",
    antallMaanederTilbake: Int,
    inntektType: String = "LOENNSINNTEKT",
    opplysningspliktig: String = virksomhetsnummer,
): List<Inntektsinformasjon> =
    (0 until antallMaanederTilbake).map { månederTilbake ->
        val maaned = fraMaaned.minusMonths(månederTilbake.toLong())
        Inntektsinformasjon(
            maaned = maaned,
            underenhet = virksomhetsnummer,
            opplysningspliktig = opplysningspliktig,
            inntektListe =
                listOf(
                    Inntekt(
                        type = inntektType,
                        beloep = beloep,
                    ),
                ),
        )
    }

// Extension function for å filtrere måneder basert på forespurt periode
fun InntektApiUt.filtrerMaaneder(
    maanedFom: YearMonth,
    maanedTom: YearMonth,
): InntektApiUt =
    InntektApiUt(
        data = data.filter { it.maaned >= maanedFom && it.maaned <= maanedTom },
    )

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
