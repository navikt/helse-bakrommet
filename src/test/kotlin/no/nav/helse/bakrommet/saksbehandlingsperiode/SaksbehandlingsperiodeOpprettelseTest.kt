package no.nav.helse.bakrommet.saksbehandlingsperiode

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.runApplicationTest
import no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.DokumentDto
import no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.tilDto
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntektsforhold.InntektsforholdDTO
import no.nav.helse.bakrommet.sykepengesoknad.Arbeidsgiverinfo
import no.nav.helse.bakrommet.sykepengesoknad.SykepengesoknadMock
import no.nav.helse.bakrommet.sykepengesoknad.enSøknad
import no.nav.helse.bakrommet.testutils.`should equal`
import no.nav.helse.bakrommet.util.asJsonNode
import no.nav.helse.flex.sykepengesoknad.kafka.ArbeidssituasjonDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadstypeDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.*

class SaksbehandlingsperiodeOpprettelseTest {
    private companion object {
        const val FNR = "01019012349"
        const val PERSON_ID = "65hth"
    }

    @Test
    fun `oppretter saksbehandlingsperiode og henter søknader`() {
        val arbeidsgiver1 =
            Arbeidsgiverinfo(
                identifikator = "123321123",
                navn = "navn for AG 1",
            )
        val arbeidsgiver2 =
            Arbeidsgiverinfo(
                identifikator = "654321123",
                navn = "navn for AG 2",
            )

        val søknad1 = enSøknad(fnr = FNR, id = UUID.randomUUID().toString(), arbeidsgiverinfo = arbeidsgiver1).asJsonNode()
        val søknad2 =
            enSøknad(
                fnr = FNR,
                id = UUID.randomUUID().toString(),
                type = SoknadstypeDTO.SELVSTENDIGE_OG_FRILANSERE,
                arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE,
                arbeidsgiverinfo = null,
            ).asJsonNode()
        val søknad3 = enSøknad(fnr = FNR, id = UUID.randomUUID().toString(), arbeidsgiverinfo = arbeidsgiver2).asJsonNode()
        val søknad3b = enSøknad(fnr = FNR, id = UUID.randomUUID().toString(), arbeidsgiverinfo = arbeidsgiver2).asJsonNode()

        runApplicationTest(
            sykepengesoknadBackendClient =
                SykepengesoknadMock.sykepengersoknadBackendClientMock(
                    søknadIdTilSvar = setOf(søknad1, søknad2, søknad3, søknad3b).associateBy { it.søknadId },
                ),
        ) { daoer ->
            daoer.personDao.opprettPerson(FNR, PERSON_ID)

            // Opprett saksbehandlingsperiode
            client.post("/v1/$PERSON_ID/saksbehandlingsperioder") {
                bearerAuth(TestOppsett.userToken)
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    { "fom": "2023-01-01", "tom": "2023-01-31", "søknader": ["${søknad1.søknadId}", "${søknad2.søknadId}", "${søknad3.søknadId}", "${søknad3b.søknadId}"] }
                    """.trimIndent(),
                )
            }.let { response ->
                assertEquals(201, response.status.value)
            }

            // Hent alle perioder
            val allePerioder =
                client.get("/v1/$PERSON_ID/saksbehandlingsperioder") {
                    bearerAuth(TestOppsett.userToken)
                }
            assertEquals(200, allePerioder.status.value)
            val perioder: List<Saksbehandlingsperiode> = allePerioder.body()

            perioder.size `should equal` 1
            val periode = perioder.first()

            // Verifiser at dokumenter ble lagret
            val dokumenterFraDB = daoer.dokumentDao.hentDokumenterFor(periode.id)
            assertEquals(4, dokumenterFraDB.size)

            dokumenterFraDB.find { it.eksternId == søknad1.søknadId }!!.also {
                assertEquals(søknad1, it.innhold.asJsonNode())
            }
            dokumenterFraDB.find { it.eksternId == søknad2.søknadId }!!.also { dok ->
                assertEquals(søknad2, dok.innhold.asJsonNode())
                assertTrue(
                    listOf(
                        "SykepengesoknadBackendClient.kt",
                        "DokumentHenter.kt",
                        "sykepengesoknad-backend/api/v3/soknader/${søknad2.søknadId}",
                    ).all { spor -> dok.request.kilde.contains(spor) },
                    "Fant ikke alt som var forventet i ${dok.request.kilde}",
                )
            }

            // Verifiser API for dokumenter
            val dokumenter: List<DokumentDto> =
                client.get("/v1/$PERSON_ID/saksbehandlingsperioder/${periode.id}/dokumenter") {
                    bearerAuth(TestOppsett.userToken)
                }.body()
            assertEquals(dokumenterFraDB.map { it.tilDto() }.toSet(), dokumenter.toSet())
        }
    }

    @Test
    fun `verifiserer automatisk genererte inntektsforhold fra søknader`() {
        val arbeidsgiver1 = Arbeidsgiverinfo(identifikator = "123321123", navn = "navn for AG 1")
        val arbeidsgiver2 = Arbeidsgiverinfo(identifikator = "654321123", navn = "navn for AG 2")

        val søknad1 = enSøknad(fnr = FNR, id = UUID.randomUUID().toString(), arbeidsgiverinfo = arbeidsgiver1).asJsonNode()
        val søknad2 =
            enSøknad(
                fnr = FNR,
                id = UUID.randomUUID().toString(),
                type = SoknadstypeDTO.SELVSTENDIGE_OG_FRILANSERE,
                arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE,
                arbeidsgiverinfo = null,
            ).asJsonNode()
        val søknad3 = enSøknad(fnr = FNR, id = UUID.randomUUID().toString(), arbeidsgiverinfo = arbeidsgiver2).asJsonNode()

        runApplicationTest(
            sykepengesoknadBackendClient =
                SykepengesoknadMock.sykepengersoknadBackendClientMock(
                    søknadIdTilSvar = setOf(søknad1, søknad2, søknad3).associateBy { it.søknadId },
                ),
        ) { daoer ->
            daoer.personDao.opprettPerson(FNR, PERSON_ID)

            client.post("/v1/$PERSON_ID/saksbehandlingsperioder") {
                bearerAuth(TestOppsett.userToken)
                contentType(ContentType.Application.Json)
                setBody(
                    """{ "fom": "2023-01-01", "tom": "2023-01-31", "søknader": ["${søknad1.søknadId}", "${søknad2.søknadId}", "${søknad3.søknadId}"] }""",
                )
            }

            val periode =
                client.get("/v1/$PERSON_ID/saksbehandlingsperioder") {
                    bearerAuth(TestOppsett.userToken)
                }.body<List<Saksbehandlingsperiode>>().first()

            // Verifiser inntektsforhold
            val inntektsforhold =
                client.get("/v1/$PERSON_ID/saksbehandlingsperioder/${periode.id}/inntektsforhold") {
                    bearerAuth(TestOppsett.userToken)
                }.body<List<InntektsforholdDTO>>()

            assertEquals(3, inntektsforhold.size)
            assertEquals(
                setOf("123321123", null, "654321123"),
                inntektsforhold.map { it.kategorisering["ORGNUMMER"]?.asText() }.toSet(),
            )

            val arbgiver1Inntektsforhold = inntektsforhold.find { it.kategorisering["ORGNUMMER"]?.asText() == arbeidsgiver1.identifikator }!!
            val forventetKategorisering = """{"INNTEKTSKATEGORI": "ARBEIDSTAKER","ORGNUMMER":"123321123"}""".asJsonNode()
            assertEquals(forventetKategorisering, arbgiver1Inntektsforhold.kategorisering)
        }
    }

    private val JsonNode.søknadId: String get() = this["id"].asText()
}
