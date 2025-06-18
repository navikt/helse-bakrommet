package no.nav.helse.bakrommet.saksbehandlingsperiode

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import no.nav.helse.bakrommet.*
import no.nav.helse.bakrommet.ainntekt.AInntektMock
import no.nav.helse.bakrommet.ainntekt.etInntektSvar
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntektsforhold.InntektsforholdDTO
import no.nav.helse.bakrommet.sykepengesoknad.Arbeidsgiverinfo
import no.nav.helse.bakrommet.sykepengesoknad.SykepengesoknadMock
import no.nav.helse.bakrommet.sykepengesoknad.enSøknad
import no.nav.helse.bakrommet.testutils.`should equal`
import no.nav.helse.bakrommet.util.asJsonNode
import no.nav.helse.bakrommet.util.somListe
import no.nav.helse.flex.sykepengesoknad.kafka.ArbeidssituasjonDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadstypeDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.*

class SaksbehandlingFlytTest {
    private companion object {
        val fnr = "01019012349"
        val personId = "65hth"
    }

    @Test
    fun `oppretter saksbehandlingsperiode, henter dokumenter og oppretter inntektsforhold`() {
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
        val søknad1 =
            UUID.randomUUID().toString().let {
                it to enSøknad(fnr = fnr, id = it, arbeidsgiverinfo = arbeidsgiver1)
            }
        val søknad2 =
            UUID.randomUUID().toString().let {
                it to
                    enSøknad(
                        fnr = fnr,
                        id = it,
                        type = SoknadstypeDTO.SELVSTENDIGE_OG_FRILANSERE,
                        arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE,
                        arbeidsgiverinfo = null,
                    )
            }
        val søknad3 =
            UUID.randomUUID().toString().let {
                it to enSøknad(fnr = fnr, id = it, arbeidsgiverinfo = arbeidsgiver2)
            }
        val søknad3b =
            UUID.randomUUID().toString().let {
                it to enSøknad(fnr = fnr, id = it, arbeidsgiverinfo = arbeidsgiver2)
            }

        val inntekter = fnr to etInntektSvar(fnr = fnr)
        runApplicationTest(
            sykepengesoknadBackendClient =
                SykepengesoknadMock.sykepengersoknadBackendClientMock(
                    søknadIdTilSvar =
                        mapOf(
                            søknad1,
                            søknad2,
                            søknad3,
                            søknad3b,
                        ),
                ),
            aInntektClient =
                AInntektMock.aInntektClientMock(
                    fnrTilSvar = mapOf(inntekter),
                ),
        ) {
            it.personDao.opprettPerson(fnr, personId)
            val response =
                client.post("/v1/$personId/saksbehandlingsperioder") {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        { "fom": "2023-01-01", "tom": "2023-01-31", "søknader": ["${søknad1.first}", "${søknad2.first}", "${søknad3.first}", "${søknad3b.first}"] }
                        """.trimIndent(),
                    )
                }
            assertEquals(201, response.status.value)

            val allePerioder =
                client.get("/v1/$personId/saksbehandlingsperioder") {
                    bearerAuth(TestOppsett.userToken)
                }
            assertEquals(200, allePerioder.status.value)
            val perioder: List<Saksbehandlingsperiode> = allePerioder.bodyAsText().somListe()

            perioder.size `should equal` 1
            val periode = perioder.first()
            val dokumenterFraDB = it.dokumentDao.hentDokumenterFor(periode.id)

            assertEquals(5, dokumenterFraDB.size)
            dokumenterFraDB.find { it.eksternId == søknad1.first }!!.also {
                assertEquals(søknad1.second.asJsonNode(), it.innhold.asJsonNode())
            }
            dokumenterFraDB.find { it.eksternId == søknad2.first }!!.also { dok ->
                assertEquals(søknad2.second.asJsonNode(), dok.innhold.asJsonNode())
                assertTrue(
                    listOf(
                        "SykepengesoknadBackendClient.kt",
                        "DokumentHenter.kt",
                        "sykepengesoknad-backend/api/v3/soknader/${søknad2.first}",
                    ).all { spor ->
                        dok.request.kilde.contains(spor)
                    },
                    "Fant ikke alt som var forventet i ${dok.request.kilde}",
                ) // TODO: Outsource til egen dedikert test?
            }
            dokumenterFraDB.find { it.eksternId == søknad3.first }!!.also {
                assertEquals(søknad3.second.asJsonNode(), it.innhold.asJsonNode())
            }
            dokumenterFraDB.find { it.eksternId == søknad3b.first }!!.also {
                assertEquals(søknad3b.second.asJsonNode(), it.innhold.asJsonNode())
            }
            dokumenterFraDB.find { it.dokumentType == "ainntekt828" }!!.also { dok ->
                assertEquals(inntekter.second.asJsonNode(), dok.innhold.asJsonNode())
                assertTrue(
                    listOf(
                        "AInntektClient.kt",
                        "DokumentHenter.kt",
                        "/rs/api/v1/hentinntektliste",
                        "8-28",
                        fnr,
                        "imagename",
                    ).all { spor ->
                        dok.request.kilde.contains(spor)
                    },
                    "Fant ikke alt som var forventet i ${dok.request.kilde}",
                ) // TODO: Outsource til egen dedikert test?
            }

            val dokumenter: List<DokumentDto> =
                client.get("/v1/$personId/saksbehandlingsperioder/${periode.id}/dokumenter") {
                    bearerAuth(TestOppsett.userToken)
                }.bodyAsText().somListe()

            assertEquals(dokumenterFraDB.map { it.tilDto() }.toSet(), dokumenter.toSet())

            val arbgiver1ForventetKategorisering =
                """
                {
                     "INNTEKTSKATEGORI": "ARBEIDSTAKER",
                     "ORGNUMMER":"123321123",
                     "ORGNAVN":"navn for AG 1"
                }
                """.trimIndent().asJsonNode()

            client.get("/v1/$personId/saksbehandlingsperioder/${periode.id}/inntektsforhold") {
                bearerAuth(TestOppsett.userToken)
            }.bodyAsText().somListe<InntektsforholdDTO>().also { inntektsforhold ->
                assertEquals(3, inntektsforhold.size)
                assertEquals(
                    setOf("123321123", null, "654321123"),
                    inntektsforhold.map { it.kategorisering["ORGNUMMER"]?.asText() }.toSet(),
                )

                inntektsforhold.find { it.kategorisering["ORGNUMMER"]?.asText() == arbeidsgiver1.identifikator }!!
                    .apply {
                        assertEquals(arbgiver1ForventetKategorisering, kategorisering)
                        assertEquals(
                            listOf(dokumenterFraDB.find { it.eksternId == søknad1.first }!!.id),
                            this.generertFraDokumenter,
                        )
                    }
            }

            it.inntektsforholdDao.hentInntektsforholdFor(periode).also { inntektsforholdFraDB ->
                inntektsforholdFraDB.find { it.kategorisering["ORGNUMMER"]?.asText() == arbeidsgiver1.identifikator }!!
                    .apply {
                        assertEquals(arbgiver1ForventetKategorisering, kategorisering)
                        assertEquals(
                            listOf(dokumenterFraDB.find { it.eksternId == søknad1.first }!!.id),
                            this.generertFraDokumenter,
                        )
                    }

                val arbgiver2ForventetKategorisering =
                    """
                    {
                         "INNTEKTSKATEGORI": "ARBEIDSTAKER",
                         "ORGNUMMER":"${arbeidsgiver2.identifikator}",
                         "ORGNAVN":"${arbeidsgiver2.navn}"
                    }
                    """.trimIndent().asJsonNode()

                inntektsforholdFraDB.find { it.kategorisering["ORGNUMMER"]?.asText() == arbeidsgiver2.identifikator }!!
                    .apply {
                        assertEquals(arbgiver2ForventetKategorisering, kategorisering)
                        val dokIder =
                            dokumenterFraDB.filter { it.eksternId in listOf(søknad3.first, søknad3b.first) }
                                .map { it.id }
                        assertEquals(2, dokIder.size)
                        assertEquals(
                            dokIder.toSet(),
                            this.generertFraDokumenter.toSet(),
                        )
                        assertEquals(kategorisering, kategoriseringGenerert)
                        assertEquals(dagoversikt, dagoversiktGenerert)
                        assertTrue(sykmeldtFraForholdet)
                    }
            }
        }
    }
}
