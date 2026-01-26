package no.nav.helse.bakrommet.e2e.behandling

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.http.HttpStatusCode
import no.nav.helse.bakrommet.api.dto.behandling.BehandlingDto
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.TypeArbeidstakerDto
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.YrkesaktivitetKategoriseringDto
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.maybeOrgnummer
import no.nav.helse.bakrommet.asJsonNode
import no.nav.helse.bakrommet.domain.enNaturligIdent
import no.nav.helse.bakrommet.domain.etOrganisasjonsnummer
import no.nav.helse.bakrommet.e2e.TestOppsett
import no.nav.helse.bakrommet.e2e.runApplicationTest
import no.nav.helse.bakrommet.e2e.sykepengesoknad.Arbeidsgiverinfo
import no.nav.helse.bakrommet.e2e.sykepengesoknad.enSøknad
import no.nav.helse.bakrommet.e2e.testutils.ApiResult
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.*
import no.nav.helse.bakrommet.e2e.testutils.`should equal`
import no.nav.helse.bakrommet.sykepengesoknad.SykepengesoknadMock
import no.nav.helse.februar
import no.nav.helse.flex.sykepengesoknad.kafka.ArbeidssituasjonDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadstypeDTO
import no.nav.helse.januar
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SaksbehandlingsperiodeOpprettelseTest {
    private val naturligIdent1 = enNaturligIdent()
    private val naturligIdent2 = enNaturligIdent()

    @Test
    fun `oppretter saksbehandlingsperiode og henter søknader`() {
        val arbeidsgiver1 =
            Arbeidsgiverinfo(
                identifikator = etOrganisasjonsnummer(),
                navn = "navn for AG 1",
            )
        val arbeidsgiver2 =
            Arbeidsgiverinfo(
                identifikator = etOrganisasjonsnummer(),
                navn = "navn for AG 2",
            )

        val søknad1 = enSøknad(fnr = naturligIdent1.value, id = UUID.randomUUID().toString(), arbeidsgiverinfo = arbeidsgiver1).asJsonNode()
        val søknad2 =
            enSøknad(
                fnr = naturligIdent1.value,
                id = UUID.randomUUID().toString(),
                type = SoknadstypeDTO.SELVSTENDIGE_OG_FRILANSERE,
                arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE,
                arbeidsgiverinfo = null,
            ).asJsonNode()
        val søknad3 = enSøknad(fnr = naturligIdent1.value, id = UUID.randomUUID().toString(), arbeidsgiverinfo = arbeidsgiver2).asJsonNode()
        val søknad3b = enSøknad(fnr = naturligIdent1.value, id = UUID.randomUUID().toString(), arbeidsgiverinfo = arbeidsgiver2).asJsonNode()

        runApplicationTest(
            sykepengesøknadProvider =
                SykepengesoknadMock.sykepengersoknadBackendClientMock(
                    søknadIdTilSvar = setOf(søknad1, søknad2, søknad3, søknad3b).associateBy { it.søknadId },
                    tokenUtvekslingProvider = TestOppsett.oboClient,
                ),
        ) {
            val personPseudoId = personsøk(naturligIdent1)

            // Opprett saksbehandlingsperiode
            val behandling =
                opprettBehandlingOgForventOk(
                    personPseudoId,
                    fom = 1.januar(2023),
                    tom = 31.januar(2023),
                    søknader = listOf(søknad1.søknadId, søknad2.søknadId, søknad3.søknadId, søknad3b.søknadId).map { UUID.fromString(it) },
                )

            val behandlinger = hentBehandlingerForPerson(personPseudoId)

            behandlinger.size `should equal` 1
            assertEquals(behandling.id, behandlinger.first().id)

            val dokumenter = hentDokumenter(personPseudoId, behandling.id)
            assertEquals(4, dokumenter.size)
            dokumenter.single { it.eksternId == søknad1.søknadId }.also {
                assertEquals(søknad1, it.innhold)
            }
            dokumenter.single { it.eksternId == søknad2.søknadId }.also { dok ->
                assertEquals(søknad2, dok.innhold)
                assertTrue(
                    listOf(
                        "SykepengesoknadBackendClient.kt",
                        "DokumentHenter.kt",
                        "sykepengesoknad-backend/api/v3/soknader/${søknad2.søknadId}",
                    ).all { spor -> dok.request.kilde.contains(spor) },
                    "Fant ikke alt som var forventet i ${dok.request.kilde}",
                )
            }
        }
    }

    @Test
    fun `verifiserer automatisk genererte yrkesaktivitet fra søknader`() {
        val arbeidsgiver1 = Arbeidsgiverinfo(identifikator = etOrganisasjonsnummer(), navn = "navn for AG 1")
        val arbeidsgiver2 = Arbeidsgiverinfo(identifikator = etOrganisasjonsnummer(), navn = "navn for AG 2")

        val søknad1 = enSøknad(fnr = naturligIdent1.value, id = UUID.randomUUID().toString(), arbeidsgiverinfo = arbeidsgiver1).asJsonNode()
        val søknad2 =
            enSøknad(
                fnr = naturligIdent1.value,
                id = UUID.randomUUID().toString(),
                type = SoknadstypeDTO.SELVSTENDIGE_OG_FRILANSERE,
                arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE,
                arbeidsgiverinfo = null,
            ).asJsonNode()
        val søknad3 = enSøknad(fnr = naturligIdent1.value, id = UUID.randomUUID().toString(), arbeidsgiverinfo = arbeidsgiver2).asJsonNode()

        runApplicationTest(
            sykepengesøknadProvider =
                SykepengesoknadMock.sykepengersoknadBackendClientMock(
                    tokenUtvekslingProvider = TestOppsett.oboClient,
                    søknadIdTilSvar = setOf(søknad1, søknad2, søknad3).associateBy { it.søknadId },
                ),
        ) {
            val personPseudoId = personsøk(naturligIdent1)

            // Opprett saksbehandlingsperiode
            val periode =
                opprettBehandlingOgForventOk(
                    personPseudoId,
                    fom = 1.januar(2023),
                    tom = 31.januar(2023),
                    søknader = listOf(søknad1.søknadId, søknad2.søknadId, søknad3.søknadId).map { UUID.fromString(it) },
                )

            // Verifiser yrkesaktivitet
            val yrkesaktivitet = hentYrkesaktiviteter(personPseudoId, periode.id)

            assertEquals(3, yrkesaktivitet.size)
            assertEquals(
                setOf(arbeidsgiver1.identifikator, null, arbeidsgiver2.identifikator),
                yrkesaktivitet.map { it.kategorisering.maybeOrgnummer() }.toSet(),
            )

            val arbgiver1Yrkesaktivitet =
                yrkesaktivitet.single { it.kategorisering.maybeOrgnummer() == arbeidsgiver1.identifikator }
            val kategorisering = arbgiver1Yrkesaktivitet.kategorisering
            assertIs<YrkesaktivitetKategoriseringDto.Arbeidstaker>(kategorisering)

            assertEquals(true, kategorisering.sykmeldt)
            val typeArbeidstaker = kategorisering.typeArbeidstaker
            assertIs<TypeArbeidstakerDto.Ordinær>(typeArbeidstaker)
            assertEquals(arbeidsgiver1.identifikator, typeArbeidstaker.orgnummer)
        }
    }

    @Test
    fun `saksbehandlingsperioder for samme person skal ikke kunne overlappe`() {
        runApplicationTest {
            val personPseudoId1 = personsøk(naturligIdent1)
            val personPseudoId2 = personsøk(naturligIdent2)

            opprettBehandling(personPseudoId1, 1.januar(2023), 31.januar(2023))
            opprettBehandling(personPseudoId1, 1.februar(2023), 15.februar(2023))
            val result1 = opprettBehandling(personPseudoId1, 15.februar(2023), 25.februar(2023))
            assertIs<ApiResult.Error>(result1)
            assertEquals(HttpStatusCode.BadRequest.value, result1.problemDetails.status)
            assertEquals("Angitte datoer overlapper med en eksisterende periode", result1.problemDetails.title)
            assertEquals(null, result1.problemDetails.detail)

            val result2 = opprettBehandling(personPseudoId1, 16.februar(2023), 25.februar(2023))
            assertIs<ApiResult.Success<BehandlingDto>>(result2)

            val resultOpprettetOverlappendeForAnnenPerson = opprettBehandling(personPseudoId2, 15.februar(2023), 25.februar(2023))
            assertIs<ApiResult.Success<BehandlingDto>>(resultOpprettetOverlappendeForAnnenPerson)
        }
    }

    private val JsonNode.søknadId: String get() = this["id"].asText()
}
