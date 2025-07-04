package no.nav.helse.bakrommet.ainntekt

import io.ktor.client.engine.mock.*
import io.ktor.http.*
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.TestOppsett.oboTokenFor
import no.nav.helse.bakrommet.bodyToJson
import no.nav.helse.bakrommet.mockHttpClient
import org.junit.jupiter.api.assertNotNull
import org.slf4j.LoggerFactory

object AInntektMock {
    private val log = LoggerFactory.getLogger(AInntektMock::class.java)

    object Person1 {
        val fnr = "08088811111"
        val resp = "{}"
    }

    fun ainntektMockHttpClient(fnrTilSvar: Map<String, String> = mapOf(Person1.fnr to Person1.resp)) =
        mockHttpClient { request ->
            val auth = request.headers[HttpHeaders.Authorization]!!
            if (auth != "Bearer ${TestOppsett.configuration.ainntekt.scope.oboTokenFor()}") {
                respondError(HttpStatusCode.Unauthorized)
            } else {
                log.info("URL: " + request.url)
                log.info("BODY: " + String(request.body.toByteArray()))
                log.info("PARAMS: " + request.url.parameters)
                log.info("HEADERS: " + request.headers)

                val payload = request.bodyToJson()
                assertNotNull(request.headers["Nav-Call-Id"])

                val fnr = payload["ident"]["identifikator"].asText()

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

    fun aInntektClientMock(fnrTilSvar: Map<String, String> = mapOf(Person1.fnr to Person1.resp)) =
        AInntektClient(
            configuration = TestOppsett.configuration.ainntekt,
            oboClient = TestOppsett.oboClient,
            httpClient = ainntektMockHttpClient(fnrTilSvar),
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
