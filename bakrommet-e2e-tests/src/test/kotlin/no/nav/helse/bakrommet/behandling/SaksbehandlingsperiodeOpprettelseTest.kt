package no.nav.helse.bakrommet.behandling

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.helse.bakrommet.TestOppsett
import no.nav.helse.bakrommet.api.dokumenter.tilDokumentDto
import no.nav.helse.bakrommet.api.dto.behandling.BehandlingDto
import no.nav.helse.bakrommet.api.dto.dokumenter.DokumentDto
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.TypeArbeidstakerDto
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.YrkesaktivitetDto
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.YrkesaktivitetKategoriseringDto
import no.nav.helse.bakrommet.asJsonNode
import no.nav.helse.bakrommet.domain.person.NaturligIdent
import no.nav.helse.bakrommet.runApplicationTest
import no.nav.helse.bakrommet.sykepengesoknad.Arbeidsgiverinfo
import no.nav.helse.bakrommet.sykepengesoknad.SykepengesoknadMock
import no.nav.helse.bakrommet.sykepengesoknad.enSøknad
import no.nav.helse.bakrommet.testutils.`should equal`
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
        val PERSON_PSEUDO_ID = UUID.nameUUIDFromBytes(PERSON_ID.toByteArray())

        const val FNR2 = "01019022222"
        const val PERSON_ID2 = "66hth"
        val PERSON_PSEUDO_ID2 = UUID.nameUUIDFromBytes(PERSON_ID2.toByteArray())
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
            sykepengesøknadProvider =
                SykepengesoknadMock.sykepengersoknadBackendClientMock(
                    søknadIdTilSvar = setOf(søknad1, søknad2, søknad3, søknad3b).associateBy { it.søknadId },
                    tokenUtvekslingProvider = TestOppsett.oboClient,
                ),
        ) { daoer ->
            daoer.personPseudoIdDao.opprettPseudoId(PERSON_PSEUDO_ID, NaturligIdent(FNR))

            // Opprett saksbehandlingsperiode
            client
                .post("/v1/${PERSON_PSEUDO_ID}/behandlinger") {
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
                client.get("/v1/${PERSON_PSEUDO_ID}/behandlinger") {
                    bearerAuth(TestOppsett.userToken)
                }
            assertEquals(200, allePerioder.status.value)
            val perioder: List<BehandlingDto> = allePerioder.body()

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
                    ).all { spor -> dok.sporing.kilde.contains(spor) },
                    "Fant ikke alt som var forventet i ${dok.sporing.kilde}",
                )
            }

            // Verifiser API for dokumenter
            val dokumenter: List<DokumentDto> =
                client
                    .get("/v1/${PERSON_PSEUDO_ID}/behandlinger/${periode.id}/dokumenter") {
                        bearerAuth(TestOppsett.userToken)
                    }.body()
            assertEquals(dokumenterFraDB.map { it.tilDokumentDto() }.toSet(), dokumenter.toSet())
        }
    }

    @Test
    fun `verifiserer automatisk genererte yrkesaktivitet fra søknader`() {
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
            sykepengesøknadProvider =
                SykepengesoknadMock.sykepengersoknadBackendClientMock(
                    tokenUtvekslingProvider = TestOppsett.oboClient,
                    søknadIdTilSvar = setOf(søknad1, søknad2, søknad3).associateBy { it.søknadId },
                ),
        ) { daoer ->
            daoer.personPseudoIdDao.opprettPseudoId(PERSON_PSEUDO_ID, NaturligIdent(FNR))
            client.post("/v1/${PERSON_PSEUDO_ID}/behandlinger") {
                bearerAuth(TestOppsett.userToken)
                contentType(ContentType.Application.Json)
                setBody(
                    """{ "fom": "2023-01-01", "tom": "2023-01-31", "søknader": ["${søknad1.søknadId}", "${søknad2.søknadId}", "${søknad3.søknadId}"] }""",
                )
            }

            val periode =
                client
                    .get("/v1/${PERSON_PSEUDO_ID}/behandlinger") {
                        bearerAuth(TestOppsett.userToken)
                    }.body<List<BehandlingDto>>()
                    .first()

            // Verifiser yrkesaktivitet
            val yrkesaktivitet =
                client
                    .get("/v1/${PERSON_PSEUDO_ID}/behandlinger/${periode.id}/yrkesaktivitet") {
                        bearerAuth(TestOppsett.userToken)
                    }.body<List<YrkesaktivitetDto>>()

            assertEquals(3, yrkesaktivitet.size)
            assertEquals(
                setOf("123321123", null, "654321123"),
                yrkesaktivitet
                    .map {
                        when (val k = it.kategorisering) {
                            is YrkesaktivitetKategoriseringDto.Arbeidstaker -> {
                                when (val type = k.typeArbeidstaker) {
                                    is TypeArbeidstakerDto.Ordinær -> type.orgnummer
                                    is TypeArbeidstakerDto.Maritim -> type.orgnummer
                                    is TypeArbeidstakerDto.Fisker -> type.orgnummer
                                    else -> null
                                }
                            }

                            is YrkesaktivitetKategoriseringDto.Frilanser -> {
                                k.orgnummer
                            }

                            else -> {
                                null
                            }
                        }
                    }.toSet(),
            )

            val arbgiver1Yrkesaktivitet =
                yrkesaktivitet.find {
                    when (val k = it.kategorisering) {
                        is YrkesaktivitetKategoriseringDto.Arbeidstaker -> {
                            when (val type = k.typeArbeidstaker) {
                                is TypeArbeidstakerDto.Ordinær -> type.orgnummer == arbeidsgiver1.identifikator
                                is TypeArbeidstakerDto.Maritim -> type.orgnummer == arbeidsgiver1.identifikator
                                is TypeArbeidstakerDto.Fisker -> type.orgnummer == arbeidsgiver1.identifikator
                                else -> false
                            }
                        }

                        is YrkesaktivitetKategoriseringDto.Frilanser -> {
                            k.orgnummer == arbeidsgiver1.identifikator
                        }

                        else -> {
                            false
                        }
                    }
                }!!

            assertTrue(
                arbgiver1Yrkesaktivitet.kategorisering is YrkesaktivitetKategoriseringDto.Arbeidstaker,
                "Kategorisering skal være Arbeidstaker",
            )
            val arbeidstakerKategorisering = arbgiver1Yrkesaktivitet.kategorisering as YrkesaktivitetKategoriseringDto.Arbeidstaker
            assertEquals(true, arbeidstakerKategorisering.sykmeldt)
            assertTrue(arbeidstakerKategorisering.typeArbeidstaker is TypeArbeidstakerDto.Ordinær)
            val ordinærType = arbeidstakerKategorisering.typeArbeidstaker as TypeArbeidstakerDto.Ordinær
            assertEquals("123321123", ordinærType.orgnummer)
        }
    }

    @Test
    fun `saksbehandlingsperioder for samme person skal ikke kunne overlappe`() {
        runApplicationTest { daoer ->
            daoer.personPseudoIdDao.opprettPseudoId(PERSON_PSEUDO_ID, NaturligIdent(FNR))
            daoer.personPseudoIdDao.opprettPseudoId(PERSON_PSEUDO_ID2, NaturligIdent(FNR2))

            suspend fun opprettPeriode(
                personPseudoId: UUID,
                fom: String,
                tom: String,
            ) = client.post("/v1/$personPseudoId/behandlinger") {
                bearerAuth(TestOppsett.userToken)
                contentType(ContentType.Application.Json)
                setBody(
                    """{ "fom": "$fom", "tom": "$tom", "søknader": [] }""",
                )
            }

            opprettPeriode(PERSON_PSEUDO_ID, "2023-01-01", "2023-01-31").apply {
                assertEquals(HttpStatusCode.Created, status)
            }
            opprettPeriode(PERSON_PSEUDO_ID, "2023-02-01", "2023-02-15").apply {
                assertEquals(HttpStatusCode.Created, status)
            }
            opprettPeriode(PERSON_PSEUDO_ID, "2023-02-15", "2023-02-25").apply {
                assertEquals(HttpStatusCode.BadRequest, status)
                assertEquals("Angitte datoer overlapper med en eksisterende periode", bodyAsText().asJsonNode()["title"].asText())
            }
            opprettPeriode(PERSON_PSEUDO_ID, "2023-02-16", "2023-02-25").apply {
                assertEquals(
                    HttpStatusCode.Created,
                    status,
                    "Nytt forsøk med justert FOM skal fungere",
                )
            }
            opprettPeriode(PERSON_PSEUDO_ID2, "2023-02-15", "2023-02-25").apply {
                assertEquals(
                    HttpStatusCode.Created,
                    status,
                    "Overlapp for annen person skal selvfølgelig fungere",
                )
            }
        }
    }

    private val JsonNode.søknadId: String get() = this["id"].asText()
}
