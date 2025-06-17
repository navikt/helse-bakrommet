package no.nav.helse.bakrommet.sykepengesoknad

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.helse.bakrommet.*
import no.nav.helse.bakrommet.TestOppsett.oboTokenFor
import no.nav.helse.bakrommet.util.logg
import no.nav.helse.bakrommet.util.objectMapper
import no.nav.helse.flex.sykepengesoknad.kafka.ArbeidssituasjonDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadstypeDTO
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SoknaderTest {
    val mockSoknaderClient =
        mockHttpClient { request ->
            val auth = request.headers[HttpHeaders.Authorization]!!
            if (auth != "Bearer " + TestOppsett.configuration.sykepengesoknadBackend.scope.oboTokenFor()) {
                logg.error("feil header: $auth")
                respondError(HttpStatusCode.Unauthorized)
            } else {
                if (request.url.toString().contains("/api/v3/soknader/") && !request.url.toString().endsWith("/api/v3/soknader")) {
                    // Single søknad request
                    val soknadId = request.url.toString().split("/").last()
                    if (soknadId == "b8079801-ff72-3e31-ad48-118df088343b") {
                        respond(
                            status = HttpStatusCode.OK,
                            content = enSøknad(),
                            headers = headersOf("Content-Type" to listOf("application/json")),
                        )
                    } else {
                        respondError(HttpStatusCode.NotFound)
                    }
                } else {
                    // Multiple søknader request
                    val fnr = request.bodyToJson()["fnr"].asText()
                    val fom = request.bodyToJson()["fom"]?.asText()

                    val reply =
                        if ((fom != null && LocalDate.parse(fom) > LocalDate.parse("2025-03-30")) || fnr != SoknaderTest.fnr) {
                            "[]"
                        } else {
                            "[${enSøknad()}]"
                        }
                    respond(
                        status = HttpStatusCode.OK,
                        content = reply,
                        headers = headersOf("Content-Type" to listOf("application/json")),
                    )
                }
            }
        }

    companion object {
        val fnr = "01019012322"
        val personId = "abcde"
    }

    @Test
    fun `henter en enkelt søknad`() =
        runApplicationTest(
            sykepengesoknadBackendClient =
                SykepengesoknadBackendClient(
                    configuration = TestOppsett.configuration.sykepengesoknadBackend,
                    httpClient = mockSoknaderClient,
                    oboClient = TestOppsett.oboClient,
                ),
        ) {
            it.personDao.opprettPerson(fnr, personId)

            client.get("/v1/$personId/soknader/b8079801-ff72-3e31-ad48-118df088343b") {
                bearerAuth(TestOppsett.userToken)
            }.apply {
                assertEquals(200, status.value)
                assertEquals(enSøknad().toJson(), bodyAsText().toJson())
            }

            // Test not found case
            client.get("/v1/$personId/soknader/non-existent-id") {
                bearerAuth(TestOppsett.userToken)
            }.apply {
                assertEquals(404, status.value)
            }
        }

    @Test
    fun `henter søknader uten fom-dato`() =
        runApplicationTest(
            sykepengesoknadBackendClient =
                SykepengesoknadBackendClient(
                    configuration = TestOppsett.configuration.sykepengesoknadBackend,
                    httpClient = mockSoknaderClient,
                    oboClient = TestOppsett.oboClient,
                ),
        ) {
            it.personDao.opprettPerson(fnr, personId)

            client.get("/v1/$personId/soknader") {
                bearerAuth(TestOppsett.userToken)
            }.apply {
                assertEquals(200, status.value)
                assertEquals("[${enSøknad()}]".toJson(), bodyAsText().toJson())
            }
        }

    @Test
    fun `henter søknader med fom-dato`() =
        runApplicationTest(
            sykepengesoknadBackendClient =
                SykepengesoknadBackendClient(
                    configuration = TestOppsett.configuration.sykepengesoknadBackend,
                    httpClient = mockSoknaderClient,
                    oboClient = TestOppsett.oboClient,
                ),
        ) {
            it.personDao.opprettPerson(fnr, personId)

            client.get("/v1/$personId/soknader?fom=2025-04-01") {
                bearerAuth(TestOppsett.userToken)
            }.apply {
                assertEquals(200, status.value)
                assertEquals("[]", bodyAsText())
            }

            client.get("/v1/$personId/soknader?fom=2025-01-01") {
                bearerAuth(TestOppsett.userToken)
            }.apply {
                assertEquals(200, status.value)
                assertEquals("[${enSøknad()}]".toJson(), bodyAsText().toJson())
            }
        }
}

fun String.toJson() = objectMapper.readTree(this)

@Language("JSON")
fun enSøknad(
    fnr: String = SoknaderTest.fnr,
    id: String = "b8079801-ff72-3e31-ad48-118df088343b",
    type: SoknadstypeDTO = SoknadstypeDTO.ARBEIDSTAKERE,
    arbeidssituasjon: ArbeidssituasjonDTO = ArbeidssituasjonDTO.ARBEIDSTAKER,
) = """
    {
        "id": "$id",
        "type": "$type",
        "status": "SENDT",
        "fnr": "$fnr",
        "sykmeldingId": "03482797-aed5-40db-afe3-48508bc088b0",
        "arbeidsgiver": {
            "navn": "Stolt Handlende Fjellrev",
            "orgnummer": "315149363"
        },
        "arbeidssituasjon": "$arbeidssituasjon",
        "korrigerer": null,
        "korrigertAv": null,
        "soktUtenlandsopphold": false,
        "arbeidsgiverForskutterer": null,
        "fom": "2025-05-28",
        "tom": "2025-06-19",
        "dodsdato": null,
        "startSyketilfelle": "2025-05-28",
        "arbeidGjenopptatt": null,
        "friskmeldt": null,
        "sykmeldingSkrevet": "2025-05-28T02:00:00",
        "opprettet": "2025-06-04T14:35:57.036992",
        "opprinneligSendt": null,
        "sendtNav": null,
        "sendtArbeidsgiver": "2025-06-20T10:05:01.775524021",
        "egenmeldinger": null,
        "fravarForSykmeldingen": [],
        "papirsykmeldinger": [],
        "fravar": [],
        "andreInntektskilder": [],
        "soknadsperioder": [
            {
                "fom": "2025-05-28",
                "tom": "2025-06-19",
                "sykmeldingsgrad": 100,
                "faktiskGrad": null,
                "avtaltTimer": null,
                "faktiskTimer": null,
                "sykmeldingstype": "AKTIVITET_IKKE_MULIG",
                "grad": 100
            }
        ],
        "sporsmal": [],
        "avsendertype": "BRUKER",
        "ettersending": false,
        "mottaker": "ARBEIDSGIVER_OG_NAV",
        "egenmeldtSykmelding": false,
        "yrkesskade": null,
        "arbeidUtenforNorge": false,
        "harRedusertVenteperiode": null,
        "behandlingsdager": [],
        "permitteringer": [],
        "merknaderFraSykmelding": null,
        "egenmeldingsdagerFraSykmelding": null,
        "merknader": null,
        "sendTilGosys": null,
        "utenlandskSykmelding": false,
        "medlemskapVurdering": null,
        "forstegangssoknad": true,
        "tidligereArbeidsgiverOrgnummer": null,
        "fiskerBlad": null,
        "inntektFraNyttArbeidsforhold": [],
        "selvstendigNaringsdrivende": null,
        "friskTilArbeidVedtakId": null,
        "friskTilArbeidVedtakPeriode": null,
        "fortsattArbeidssoker": null,
        "inntektUnderveis": null,
        "ignorerArbeidssokerregister": null
    }
    """.trimIndent()
