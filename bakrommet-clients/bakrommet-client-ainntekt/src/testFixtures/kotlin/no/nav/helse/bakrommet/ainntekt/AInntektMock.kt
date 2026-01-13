package no.nav.helse.bakrommet.ainntekt

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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
import no.nav.helse.bakrommet.auth.AccessToken
import no.nav.helse.bakrommet.auth.OAuthScope
import no.nav.helse.bakrommet.auth.OboToken
import no.nav.helse.bakrommet.auth.TokenUtvekslingProvider
import no.nav.helse.bakrommet.infrastruktur.provider.AInntektResponse
import no.nav.helse.bakrommet.infrastruktur.provider.Inntekt
import no.nav.helse.bakrommet.infrastruktur.provider.Inntektsinformasjon
import no.nav.helse.bakrommet.infrastruktur.provider.Organisasjon
import no.nav.helse.bakrommet.util.objectMapper
import no.nav.helse.bakrommet.util.serialisertTilString
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.YearMonth

object AInntektMock {
    private val log = LoggerFactory.getLogger(AInntektMock::class.java)

    // Default test konfigurasjon
    val defaultConfiguration =
        AInntektModule.Configuration(
            hostname = "inntektskomponenten-host",
            scope = OAuthScope("inntektskomponenten-scope"),
        )

    // Default OBO client for testing
    fun createDefaultOboClient(): TokenUtvekslingProvider =
        object : TokenUtvekslingProvider {
            override suspend fun exchangeToken(
                accessToken: AccessToken,
                scope: OAuthScope,
            ): OboToken = OboToken("OBO-TOKEN_FOR_${scope.asDefaultScope()}")
        }

    fun ainntektMockHttpClient(
        configuration: AInntektModule.Configuration = defaultConfiguration,
        fnrTilAInntektResponse: Map<String, AInntektResponse>,
    ) = mockHttpClient { request ->
        val auth = request.headers[HttpHeaders.Authorization]!!
        if (auth != "Bearer ${configuration.scope.oboTokenFor()}") {
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
                val AInntektResponse = (fnrTilAInntektResponse[fnr] ?: AInntektResponse(data = emptyList())).filtrerMaaneder(maanedFom, maanedTom)

                respond(
                    status = HttpStatusCode.OK,
                    content = AInntektResponse.serialisertTilString(),
                    headers = headersOf("Content-Type" to listOf("application/json")),
                )
            }
        }
    }

    fun aInntektClientMock(
        configuration: AInntektModule.Configuration = defaultConfiguration,
        tokenUtvekslingProvider: TokenUtvekslingProvider = createDefaultOboClient(),
        fnrTilAInntektResponse: Map<String, AInntektResponse>,
        mockClient: HttpClient? = null,
    ) = AInntektClient(
        configuration = configuration,
        tokenUtvekslingProvider = tokenUtvekslingProvider,
        httpClient = mockClient ?: ainntektMockHttpClient(configuration, fnrTilAInntektResponse),
    )
}

fun etInntektSvar(
    virksomhet: String = "999999999",
    opplysningspliktig: String = "988888888",
    skjæringstidspunkt: YearMonth? = null,
    beloep: Int = 12000,
): AInntektResponse {
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

    return AInntektResponse(
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
                                beloep = BigDecimal.valueOf(beloep.toLong()),
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
fun AInntektResponse.filtrerMaaneder(
    maanedFom: YearMonth,
    maanedTom: YearMonth,
): AInntektResponse =
    AInntektResponse(
        data = data.filter { it.maaned >= maanedFom && it.maaned <= maanedTom },
    )

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

suspend fun HttpRequestData.bodyToJson(): JsonNode = jacksonObjectMapper().readValue(body.toByteArray(), JsonNode::class.java)
