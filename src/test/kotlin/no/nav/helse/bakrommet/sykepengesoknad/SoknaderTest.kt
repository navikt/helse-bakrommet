package no.nav.helse.bakrommet.sykepengesoknad

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.helse.bakrommet.*
import no.nav.helse.bakrommet.util.logg
import no.nav.helse.bakrommet.util.objectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SoknaderTest {
    val mockSoknaderClient =
        mockHttpClient { request ->
            val auth = request.headers[HttpHeaders.Authorization]!!
            if (auth != "Bearer " + TestOppsett.oboTokenFor(TestOppsett.configuration.sykepengesoknadBackend.scope)) {
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

fun enSøknad(fnr: String = SoknaderTest.fnr) =
    """
    {
        "id": "b8079801-ff72-3e31-ad48-118df088343b",
        "type": "FRISKMELDT_TIL_ARBEIDSFORMIDLING",
        "status": "NY",
        "fnr": "$fnr",
        "sykmeldingId": null,
        "arbeidsgiver": null,
        "arbeidssituasjon": null,
        "korrigerer": null,
        "korrigertAv": null,
        "soktUtenlandsopphold": false,
        "arbeidsgiverForskutterer": null,
        "fom": "2025-03-17",
        "tom": "2025-03-30",
        "dodsdato": null,
        "startSyketilfelle": "2025-03-17",
        "arbeidGjenopptatt": null,
        "friskmeldt": null,
        "sykmeldingSkrevet": null,
        "opprettet": "2025-04-09T09:49:31.037206",
        "opprinneligSendt": null,
        "sendtNav": null,
        "sendtArbeidsgiver": null,
        "egenmeldinger": null,
        "fravarForSykmeldingen": [],
        "papirsykmeldinger": [],
        "fravar": [],
        "andreInntektskilder": [],
        "soknadsperioder": [],
        "sporsmal": [],
        "avsendertype": null,
        "ettersending": false,
        "mottaker": null,
        "egenmeldtSykmelding": null,
        "yrkesskade": null,
        "arbeidUtenforNorge": null,
        "harRedusertVenteperiode": null,
        "behandlingsdager": [],
        "permitteringer": [],
        "merknaderFraSykmelding": null,
        "egenmeldingsdagerFraSykmelding": null,
        "merknader": null,
        "sendTilGosys": null,
        "utenlandskSykmelding": false,
        "medlemskapVurdering": null,
        "forstegangssoknad": false,
        "tidligereArbeidsgiverOrgnummer": null,
        "fiskerBlad": null,
        "inntektFraNyttArbeidsforhold": [],
        "selvstendigNaringsdrivende": null,
        "friskTilArbeidVedtakId": "fc8ea85d-6ff2-4c50-965b-5fbfe6e4c320",
        "friskTilArbeidVedtakPeriode": "{\"fom\":\"2025-03-17\",\"tom\":\"2025-04-30\"}",
        "fortsattArbeidssoker": false,
        "inntektUnderveis": false,
        "ignorerArbeidssokerregister": true
    }    
    """.trimIndent()
