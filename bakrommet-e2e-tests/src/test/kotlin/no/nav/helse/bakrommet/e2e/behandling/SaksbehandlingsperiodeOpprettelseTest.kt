package no.nav.helse.bakrommet.e2e.behandling

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import no.nav.helse.bakrommet.api.dokumenter.tilDokumentDto
import no.nav.helse.bakrommet.api.dto.behandling.OpprettBehandlingRequestDto
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.TypeArbeidstakerDto
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.YrkesaktivitetKategoriseringDto
import no.nav.helse.bakrommet.asJsonNode
import no.nav.helse.bakrommet.domain.person.NaturligIdent
import no.nav.helse.bakrommet.e2e.TestOppsett
import no.nav.helse.bakrommet.e2e.runApplicationTest
import no.nav.helse.bakrommet.e2e.sykepengesoknad.Arbeidsgiverinfo
import no.nav.helse.bakrommet.e2e.sykepengesoknad.enSøknad
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.hentBehandlingerForPerson
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.hentDokumenter
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.hentYrkesaktiviteter
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.opprettBehandling
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.personsøk
import no.nav.helse.bakrommet.e2e.testutils.`should equal`
import no.nav.helse.bakrommet.sykepengesoknad.SykepengesoknadMock
import no.nav.helse.flex.sykepengesoknad.kafka.ArbeidssituasjonDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadstypeDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class SaksbehandlingsperiodeOpprettelseTest {
    private companion object {
        const val FNR = "01019012349"
        const val FNR2 = "01019022222"
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
            val personPseudoId = personsøk(NaturligIdent(FNR))

            // Opprett saksbehandlingsperiode
            val periode =
                opprettBehandling(
                    personPseudoId,
                    OpprettBehandlingRequestDto(
                        fom = LocalDate.of(2023, 1, 1),
                        tom = LocalDate.of(2023, 1, 31),
                        søknader = listOf(søknad1.søknadId, søknad2.søknadId, søknad3.søknadId, søknad3b.søknadId).map { UUID.fromString(it) },
                    ),
                )

            // Hent alle perioder
            val perioder = hentBehandlingerForPerson(personPseudoId)

            perioder.size `should equal` 1
            assertEquals(periode.id, perioder.first().id)

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
            val dokumenter = hentDokumenter(personPseudoId, periode.id)
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
        ) { _ ->
            val personPseudoId = personsøk(NaturligIdent(FNR))

            // Opprett saksbehandlingsperiode
            val periode =
                opprettBehandling(
                    personPseudoId,
                    OpprettBehandlingRequestDto(
                        fom = LocalDate.of(2023, 1, 1),
                        tom = LocalDate.of(2023, 1, 31),
                        søknader = listOf(søknad1.søknadId, søknad2.søknadId, søknad3.søknadId).map { UUID.fromString(it) },
                    ),
                )

            // Verifiser yrkesaktivitet
            val yrkesaktivitet = hentYrkesaktiviteter(personPseudoId, periode.id)

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
        runApplicationTest { _ ->
            val personPseudoId = personsøk(NaturligIdent(FNR))
            val personPseudoId2 = personsøk(NaturligIdent(FNR2))

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

            opprettPeriode(personPseudoId, "2023-01-01", "2023-01-31").apply {
                assertEquals(HttpStatusCode.Created, status)
            }
            opprettPeriode(personPseudoId, "2023-02-01", "2023-02-15").apply {
                assertEquals(HttpStatusCode.Created, status)
            }
            opprettPeriode(personPseudoId, "2023-02-15", "2023-02-25").apply {
                assertEquals(HttpStatusCode.BadRequest, status)
                assertEquals("Angitte datoer overlapper med en eksisterende periode", bodyAsText().asJsonNode()["title"].asText())
            }
            opprettPeriode(personPseudoId, "2023-02-16", "2023-02-25").apply {
                assertEquals(
                    HttpStatusCode.Created,
                    status,
                    "Nytt forsøk med justert FOM skal fungere",
                )
            }
            opprettPeriode(personPseudoId2, "2023-02-15", "2023-02-25").apply {
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
