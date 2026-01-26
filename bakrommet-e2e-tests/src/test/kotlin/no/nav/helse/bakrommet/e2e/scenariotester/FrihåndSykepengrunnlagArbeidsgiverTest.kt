package no.nav.helse.bakrommet.e2e.scenariotester

import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.OpprettSykepengegrunnlagRequest
import no.nav.helse.bakrommet.e2e.testutils.Arbeidstaker
import no.nav.helse.bakrommet.e2e.testutils.Scenario
import no.nav.helse.bakrommet.e2e.testutils.SykAlleDager
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.hentUtbetalingsberegning
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.opprettSykepengegrunnlag
import no.nav.helse.bakrommet.e2e.testutils.`should equal`
import no.nav.helse.januar
import no.nav.helse.mars
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class FrihåndSykepengrunnlagArbeidsgiverTest {
    @Test
    fun `frihånd sykepengegrunnlag`() {
        Scenario(
            yrkesaktiviteter =
                listOf(
                    Arbeidstaker(
                        "988888888",
                        inntekt = null,
                        dagoversikt = SykAlleDager(),
                    ),
                ),
            fom = 1.januar(2021),
            tom = 10.januar(2021),
            besluttOgGodkjenn = false,
        ).runWithApplicationTestBuilder { førsteBehandling ->

            val personId = førsteBehandling.scenario.pseudoId
            val behandlingId = førsteBehandling.behandling.id

            opprettSykepengegrunnlag(
                personId,
                behandlingId,
                OpprettSykepengegrunnlagRequest(
                    beregningsgrunnlag = BigDecimal(2000000),
                    begrunnelse = "Yrkesskade 2002",
                    datoForGBegrensning = 2.mars(2002),
                    beregningskoder = listOf(),
                ),
            ).also {
                it.sykepengegrunnlag!!.sykepengegrunnlag `should equal` 308160.0
            }
            hentUtbetalingsberegning(personId, behandlingId).also { beregning ->
                beregning!!
                    .beregningData.spilleromOppdrag.oppdrag.size `should equal` 1

                beregning!!
                    .beregningData.spilleromOppdrag.oppdrag
                    .first()
                    .totalbeløp `should equal` 7110
            }
        }
    }
}
