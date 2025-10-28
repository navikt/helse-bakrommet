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
import no.nav.helse.bakrommet.util.objectMapper
import org.slf4j.LoggerFactory

object AInntektMock {
    private val log = LoggerFactory.getLogger(AInntektMock::class.java)

    object Person1 {
        val fnr = "08088811111"
        val resp = "{}"
    }

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
        fnrTilSvar: Map<String, String> = mapOf(Person1.fnr to Person1.resp),
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

            if (fnr.endsWith("403")) {
                respond(
                    status = HttpStatusCode.Forbidden,
                    content = "403",
                )
            } else {
                val svar = fnrTilSvar[fnr] ?: "{}"
                respond(
                    status = HttpStatusCode.OK,
                    content = svar,
                    headers = headersOf("Content-Type" to listOf("application/json")),
                )
            }
        }
    }

    fun aInntektClientMock(
        configuration: Configuration.AInntekt = defaultConfiguration,
        oboClient: OboClient = createDefaultOboClient(),
        fnrTilSvar: Map<String, String> = mapOf(Person1.fnr to Person1.resp),
        mockClient: HttpClient? = null,
    ) = AInntektClient(
        configuration = configuration,
        oboClient = oboClient,
        httpClient = mockClient ?: ainntektMockHttpClient(configuration, oboClient, fnrTilSvar),
    )
}

fun etInntektSvar(
    fnr: String = AInntektMock.Person1.fnr,
    virksomhet: String = "999999999",
    opplysningspliktig: String = "888888888",
) = """
    {
      "arbeidsInntektMaaned": [
        {
          "aarMaaned": "2022-09",
          "arbeidsInntektInformasjon": {
            "inntektListe": [
              {
                "inntektType": "LOENNSINNTEKT",
                "beloep": 12000.0,
                "fordel": "kontantytelse",
                "inntektskilde": "A-ordningen",
                "inntektsperiodetype": "Maaned",
                "inntektsstatus": "LoependeInnrapportert",
                "utbetaltIMaaned": "2022-09",
                "opplysningspliktig": {
                  "identifikator": "$opplysningspliktig",
                  "aktoerType": "ORGANISASJON"
                },
                "virksomhet": {
                  "identifikator": "$virksomhet",
                  "aktoerType": "ORGANISASJON"
                },
                "inntektsmottaker": {
                  "identifikator": "$fnr",
                  "aktoerType": "NATURLIG_IDENT"
                },
                "inngaarIGrunnlagForTrekk": true,
                "utloeserArbeidsgiveravgift": true,
                "informasjonsstatus": "InngaarAlltid",
                "beskrivelse": "fastloenn"
              }
            ]
          }
        },
        {
          "aarMaaned": "2022-10",
          "arbeidsInntektInformasjon": {
            "inntektListe": [
              {
                "inntektType": "LOENNSINNTEKT",
                "beloep": 12000.0,
                "fordel": "kontantytelse",
                "inntektskilde": "A-ordningen",
                "inntektsperiodetype": "Maaned",
                "inntektsstatus": "LoependeInnrapportert",
                "utbetaltIMaaned": "2022-10",
                "opplysningspliktig": {
                  "identifikator": "$opplysningspliktig",
                  "aktoerType": "ORGANISASJON"
                },
                "virksomhet": {
                  "identifikator": "$virksomhet",
                  "aktoerType": "ORGANISASJON"
                },
                "inntektsmottaker": {
                  "identifikator": "$fnr",
                  "aktoerType": "NATURLIG_IDENT"
                },
                "inngaarIGrunnlagForTrekk": true,
                "utloeserArbeidsgiveravgift": true,
                "informasjonsstatus": "InngaarAlltid",
                "beskrivelse": "fastloenn"
              }
            ]
          }
        },
        {
          "aarMaaned": "2022-08",
          "arbeidsInntektInformasjon": {
            "inntektListe": [
              {
                "inntektType": "LOENNSINNTEKT",
                "beloep": 12000.0,
                "fordel": "kontantytelse",
                "inntektskilde": "A-ordningen",
                "inntektsperiodetype": "Maaned",
                "inntektsstatus": "LoependeInnrapportert",
                "utbetaltIMaaned": "2022-08",
                "opplysningspliktig": {
                  "identifikator": "$opplysningspliktig",
                  "aktoerType": "ORGANISASJON"
                },
                "virksomhet": {
                  "identifikator": "$virksomhet",
                  "aktoerType": "ORGANISASJON"
                },
                "inntektsmottaker": {
                  "identifikator": "$fnr",
                  "aktoerType": "NATURLIG_IDENT"
                },
                "inngaarIGrunnlagForTrekk": true,
                "utloeserArbeidsgiveravgift": true,
                "informasjonsstatus": "InngaarAlltid",
                "beskrivelse": "fastloenn"
              }
            ]
          }
        },
        {
          "aarMaaned": "2022-11",
          "arbeidsInntektInformasjon": {
            "inntektListe": [
              {
                "inntektType": "LOENNSINNTEKT",
                "beloep": 12000.0,
                "fordel": "kontantytelse",
                "inntektskilde": "A-ordningen",
                "inntektsperiodetype": "Maaned",
                "inntektsstatus": "LoependeInnrapportert",
                "utbetaltIMaaned": "2022-11",
                "opplysningspliktig": {
                  "identifikator": "$opplysningspliktig",
                  "aktoerType": "ORGANISASJON"
                },
                "virksomhet": {
                  "identifikator": "$virksomhet",
                  "aktoerType": "ORGANISASJON"
                },
                "inntektsmottaker": {
                  "identifikator": "$fnr",
                  "aktoerType": "NATURLIG_IDENT"
                },
                "inngaarIGrunnlagForTrekk": true,
                "utloeserArbeidsgiveravgift": true,
                "informasjonsstatus": "InngaarAlltid",
                "beskrivelse": "fastloenn"
              }
            ]
          }
        }
      ],
      "ident": {
        "identifikator": "$fnr",
        "aktoerType": "NATURLIG_IDENT"
      }
    }    
    """.trimIndent()

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
