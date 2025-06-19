package no.nav.helse.bakrommet.saksbehandlingsperiode

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.ainntekt.AInntektMock
import no.nav.helse.bakrommet.ainntekt.etInntektSvar
import no.nav.helse.bakrommet.runApplicationTest
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntektsforhold.InntektsforholdDTO
import no.nav.helse.bakrommet.sykepengesoknad.Arbeidsgiverinfo
import no.nav.helse.bakrommet.sykepengesoknad.SykepengesoknadMock
import no.nav.helse.bakrommet.sykepengesoknad.enSøknad
import no.nav.helse.bakrommet.testutils.`should equal`
import no.nav.helse.bakrommet.util.asJsonNode
import no.nav.helse.bakrommet.util.deserialize
import no.nav.helse.flex.sykepengesoknad.kafka.ArbeidssituasjonDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadstypeDTO
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
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
            enSøknad(fnr = fnr, id = UUID.randomUUID().toString(), arbeidsgiverinfo = arbeidsgiver1).asJsonNode()
        val søknad2 =
            enSøknad(
                fnr = fnr,
                id = UUID.randomUUID().toString(),
                type = SoknadstypeDTO.SELVSTENDIGE_OG_FRILANSERE,
                arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE,
                arbeidsgiverinfo = null,
            ).asJsonNode()

        val søknad3 =
            enSøknad(fnr = fnr, id = UUID.randomUUID().toString(), arbeidsgiverinfo = arbeidsgiver2).asJsonNode()
        val søknad3b =
            enSøknad(fnr = fnr, id = UUID.randomUUID().toString(), arbeidsgiverinfo = arbeidsgiver2).asJsonNode()

        val inntekter = fnr to etInntektSvar(fnr = fnr)
        runApplicationTest(
            sykepengesoknadBackendClient =
                SykepengesoknadMock.sykepengersoknadBackendClientMock(
                    søknadIdTilSvar =
                        setOf(
                            søknad1,
                            søknad2,
                            søknad3,
                            søknad3b,
                        ).associateBy { it.søknadId },
                ),
            aInntektClient =
                AInntektMock.aInntektClientMock(
                    fnrTilSvar = mapOf(inntekter),
                ),
        ) { daoer ->
            daoer.personDao.opprettPerson(fnr, personId)
            client.post("/v1/$personId/saksbehandlingsperioder") {
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

            val allePerioder =
                client.get("/v1/$personId/saksbehandlingsperioder") {
                    bearerAuth(TestOppsett.userToken)
                }
            assertEquals(200, allePerioder.status.value)
            val perioder: List<Saksbehandlingsperiode> = allePerioder.body()

            perioder.size `should equal` 1
            val periode = perioder.first()
            val dokumenterFraDB = daoer.dokumentDao.hentDokumenterFor(periode.id)

            assertEquals(5, dokumenterFraDB.size)
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
                    ).all { spor ->
                        dok.request.kilde.contains(spor)
                    },
                    "Fant ikke alt som var forventet i ${dok.request.kilde}",
                ) // TODO: Outsource til egen dedikert test?
            }
            dokumenterFraDB.find { it.eksternId == søknad3.søknadId }!!.also {
                assertEquals(søknad3, it.innhold.asJsonNode())
            }
            dokumenterFraDB.find { it.eksternId == søknad3b.søknadId }!!.also {
                assertEquals(søknad3b, it.innhold.asJsonNode())
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
                }.body()

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
            }.body<List<InntektsforholdDTO>>().also { inntektsforhold ->
                assertEquals(3, inntektsforhold.size)
                assertEquals(
                    setOf("123321123", null, "654321123"),
                    inntektsforhold.map { it.kategorisering["ORGNUMMER"]?.asText() }.toSet(),
                )

                inntektsforhold.find { it.kategorisering["ORGNUMMER"]?.asText() == arbeidsgiver1.identifikator }!!
                    .apply {
                        assertEquals(arbgiver1ForventetKategorisering, kategorisering)
                        assertEquals(
                            listOf(dokumenterFraDB.find { it.eksternId == søknad1.søknadId }!!.id),
                            this.generertFraDokumenter,
                        )
                    }
            }

            daoer.inntektsforholdDao.hentInntektsforholdFor(periode).also { inntektsforholdFraDB ->
                inntektsforholdFraDB.find { it.kategorisering["ORGNUMMER"]?.asText() == arbeidsgiver1.identifikator }!!
                    .apply {
                        assertEquals(arbgiver1ForventetKategorisering, kategorisering)
                        assertEquals(
                            listOf(dokumenterFraDB.find { it.eksternId == søknad1.søknadId }!!.id),
                            this.generertFraDokumenter,
                        )
                    }

                val arbgiver2ForventetKategorisering =
                    """
                    {
                         "INNTEKTSKATEGORI": "ARBEIDSTAKER",
                         "ORGNUMMER": "${arbeidsgiver2.identifikator}",
                         "ORGNAVN": "${arbeidsgiver2.navn}"
                    }
                    """.trimIndent().asJsonNode()

                inntektsforholdFraDB.find { it.kategorisering["ORGNUMMER"]?.asText() == arbeidsgiver2.identifikator }!!
                    .apply {
                        assertEquals(arbgiver2ForventetKategorisering, kategorisering)
                        val dokIder =
                            dokumenterFraDB.filter { it.eksternId in listOf(søknad3.søknadId, søknad3b.søknadId) }
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

            @Language("json")
            val kategorisering =
                """
                {
                    "kategorisering": {
                        "INNTEKTSKATEGORI": "SELVSTENDIG_NÆRINGSDRIVENDE",
                        "TYPE_SELVSTENDIG_NÆRINGSDRIVENDE": "FISKER",
                        "FISKER_BLAD": "FISKER_BLAD_B",
                        "ER_SYKMELDT": "ER_SYKMELDT_JA"
                    }
                }
                """.trimIndent()

            val opprettetInntektsforholdId =
                client.post("/v1/$personId/saksbehandlingsperioder/${periode.id}/inntektsforhold") {
                    bearerAuth(TestOppsett.userToken)
                    contentType(ContentType.Application.Json)
                    setBody(kategorisering)
                }.let { response ->
                    assertEquals(201, response.status.value)
                    val body = response.body<JsonNode>()
                    assertEquals(
                        kategorisering.asJsonNode()["kategorisering"],
                        body["kategorisering"],
                    )
                    val id = body["id"].asText()
                    assertDoesNotThrow { UUID.fromString(id) }
                    assertDoesNotThrow { val dto = body.deserialize<InntektsforholdDTO>() }
                    UUID.fromString(id)
                }

            @Language("json")
            val nyKategorisering =
                """
                {
                    "INNTEKTSKATEGORI": "ARBEIDSTAKER_ELLER_NOE_SÅNT",
                    "ER_SYKMELDT": "ER_SYKMELDT_NEI"
                }
                """.trimIndent()

            client.put("/v1/$personId/saksbehandlingsperioder/${periode.id}/inntektsforhold/$opprettetInntektsforholdId/kategorisering") {
                bearerAuth(TestOppsett.userToken)
                contentType(ContentType.Application.Json)
                setBody(nyKategorisering)
            }.let { response ->
                assertEquals(204, response.status.value)
            }

            daoer.inntektsforholdDao.hentInntektsforholdFor(periode).also { inntektsforholdFraDB ->
                inntektsforholdFraDB.filter { it.id == opprettetInntektsforholdId }.also {
                    assertEquals(1, it.size)
                    assertEquals(
                        nyKategorisering.asJsonNode(),
                        it.first().kategorisering,
                    )
                }
            }
        }
    }

    private val JsonNode.søknadId: String get() = this["id"].asText()
}
