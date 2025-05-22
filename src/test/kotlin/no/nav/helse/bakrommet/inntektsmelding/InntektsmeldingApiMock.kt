package no.nav.helse.bakrommet.inntektsmelding

import io.ktor.client.engine.mock.*
import io.ktor.http.*
import no.nav.helse.bakrommet.Configuration
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.bodyToJson
import no.nav.helse.bakrommet.mockHttpClient
import org.junit.jupiter.api.assertNotNull
import org.slf4j.LoggerFactory

object InntektsmeldingApiMock {
    private val log = LoggerFactory.getLogger(InntektsmeldingApiMock::class.java)

    object Person1 {
        val fnr = "08088811111"
        val resp = "[${enInntektsmelding()}]"
    }

    fun inntektsmeldingMockHttpClient(fnrTilSvar: Map<String, String> = mapOf(Person1.fnr to Person1.resp)) =
        mockHttpClient { request ->
            val auth = request.headers[HttpHeaders.Authorization]!!
            if (auth != "Bearer ${TestOppsett.oboToken}") {
                respondError(HttpStatusCode.Unauthorized)
            } else {
                log.info("URL: " + request.url)
                log.info("BODY: " + String(request.body.toByteArray()))
                log.info("PARAMS: " + request.url.parameters)
                log.info("HEADERS: " + request.headers)

                val payload = request.bodyToJson()
                assertNotNull(request.headers["Nav-Call-Id"])

                val fnr = payload["fnr"].asText()

                if (fnr.endsWith("400")) {
                    respond(
                        status = HttpStatusCode.BadRequest,
                        content = "400",
                    )
                } else {
                    val svar = fnrTilSvar[fnr]
                    if (svar == null) {
                        respond(
                            status = HttpStatusCode.NotFound,
                            content = "404",
                        )
                    } else {
                        respond(
                            status = HttpStatusCode.OK,
                            content = svar,
                            headers = headersOf("Content-Type" to listOf("application/json")),
                        )
                    }
                }
            }
        }

    fun inntektsmeldingClientMock() =
        InntektsmeldingClient(
            configuration = Configuration.Inntektsmelding(baseUrl = "http://localhost", scope = "im-scope"),
            httpClient = inntektsmeldingMockHttpClient(),
        )

    fun enInntektsmelding() =
        """
        {
            "inntektsmeldingId": "7371c1ab-ee9b-4fb3-b540-c360fb0156a0",
            "vedtaksperiodeId": "19834fcc-859c-46c8-8552-c366e64a4109",
            "arbeidstakerFnr": "",
            "arbeidstakerAktorId": "0000111122223",
            "virksomhetsnummer": "999888777",
            "arbeidsgiverFnr": null,
            "arbeidsgiverAktorId": null,
            "innsenderFulltNavn": "BEROEMT FLYTTELASS",
            "innsenderTelefon": "11223344",
            "begrunnelseForReduksjonEllerIkkeUtbetalt": "",
            "bruttoUtbetalt": null,
            "arbeidsgivertype": "VIRKSOMHET",
            "arbeidsforholdId": null,
            "beregnetInntekt": "8876.00",
            "inntektsdato": "2025-02-01",
            "refusjon": {
              "beloepPrMnd": "0.00",
              "opphoersdato": null
            },
            "endringIRefusjoner": [],
            "opphoerAvNaturalytelser": [],
            "gjenopptakelseNaturalytelser": [],
            "arbeidsgiverperioder": [
              {
                "fom": "2025-02-01",
                "tom": "2025-02-16"
              }
            ],
            "status": "GYLDIG",
            "arkivreferanse": "im_690924579",
            "ferieperioder": [],
            "foersteFravaersdag": null,
            "mottattDato": "2025-05-05T13:58:01.818383756",
            "naerRelasjon": null,
            "avsenderSystem": {
              "navn": "NAV_NO",
              "versjon": "1.0"
            },
            "inntektEndringAarsak": {
              "aarsak": "Bonus",
              "perioder": null,
              "gjelderFra": null,
              "bleKjent": null
            },
            "inntektEndringAarsaker": [
              {
                "aarsak": "Bonus",
                "perioder": null,
                "gjelderFra": null,
                "bleKjent": null
              }
            ],
            "arsakTilInnsending": "Ny",
            "mottaksKanal": "NAV_NO",
            "format": "Arbeidsgiveropplysninger",
            "forespurt": true
          }        
        """.trimIndent()
}
