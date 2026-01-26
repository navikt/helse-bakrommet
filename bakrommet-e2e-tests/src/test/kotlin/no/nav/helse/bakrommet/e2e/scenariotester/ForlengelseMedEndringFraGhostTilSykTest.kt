package no.nav.helse.bakrommet.e2e.scenariotester

import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.YrkesaktivitetKategoriseringDto
import no.nav.helse.bakrommet.e2e.testutils.AInntekt
import no.nav.helse.bakrommet.e2e.testutils.Arbeidstaker
import no.nav.helse.bakrommet.e2e.testutils.Scenario
import no.nav.helse.bakrommet.e2e.testutils.ScenarioDefaults
import no.nav.helse.bakrommet.e2e.testutils.Selvstendig
import no.nav.helse.bakrommet.e2e.testutils.SigrunInntekt
import no.nav.helse.bakrommet.e2e.testutils.SykAlleDager
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.hentYrkesaktiviteter
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.opprettBehandlingOgForventOk
import no.nav.helse.bakrommet.e2e.testutils.`should equal`
import no.nav.helse.bakrommet.sykepengesoknad.soknad
import no.nav.helse.flex.sykepengesoknad.kafka.ArbeidssituasjonDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadstypeDTO
import org.junit.jupiter.api.Test
import java.util.UUID

class ForlengelseMedEndringFraGhostTilSykTest {
    @Test
    fun `Forlengelse med endring fra ghost til syk`() {
        val næringsdrivendesøknad =
            soknad(
                fom = ScenarioDefaults.tom.plusDays(1),
                tom = ScenarioDefaults.tom.plusDays(20),
                fnr = ScenarioDefaults.fnr,
            ) {
                type = SoknadstypeDTO.SELVSTENDIGE_OG_FRILANSERE
                arbeidssituasjon = ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE
            }
        Scenario(
            soknader = listOf(næringsdrivendesøknad),
            yrkesaktiviteter =
                listOf(
                    Arbeidstaker("988888888", inntekt = AInntekt(10000, 10000, 10000), dagoversikt = SykAlleDager()),
                    Selvstendig(inntekt = SigrunInntekt()),
                ),
        ).runWithApplicationTestBuilder { førsteBehandling ->
            val personId = førsteBehandling.scenario.pseudoId
            val nyBehandling =
                opprettBehandlingOgForventOk(
                    personId,
                    fom = ScenarioDefaults.tom.plusDays(1),
                    tom = ScenarioDefaults.tom.plusDays(20),
                    søknader = listOf(UUID.fromString(næringsdrivendesøknad.id)),
                )
            hentYrkesaktiviteter(personId, nyBehandling.id).let {
                it.size `should equal` 2
                val arbeidstaker = it.first { it.kategorisering is YrkesaktivitetKategoriseringDto.Arbeidstaker }.kategorisering as YrkesaktivitetKategoriseringDto.Arbeidstaker
                val næring = it.first { it.kategorisering is YrkesaktivitetKategoriseringDto.SelvstendigNæringsdrivende }.kategorisering as YrkesaktivitetKategoriseringDto.SelvstendigNæringsdrivende
                arbeidstaker.sykmeldt `should equal` true
                næring.sykmeldt `should equal` true
            }
        }
    }
}
