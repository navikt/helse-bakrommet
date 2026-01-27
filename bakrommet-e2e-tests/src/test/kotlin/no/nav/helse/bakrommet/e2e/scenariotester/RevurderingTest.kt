package no.nav.helse.bakrommet.e2e.scenariotester

import no.nav.helse.bakrommet.api.dto.tidslinje.TidslinjeBehandlingStatus
import no.nav.helse.bakrommet.domain.etOrganisasjonsnummer
import no.nav.helse.bakrommet.e2e.testutils.*
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.*
import org.junit.jupiter.api.Test

class RevurderingTest {
    @Test
    fun `Vi revurderer og endrer graderingen lavere`() {
        Scenario(
            yrkesaktiviteter =
                listOf(
                    Arbeidstaker(etOrganisasjonsnummer(), inntekt = AInntekt(10000, 10000, 10000), dagoversikt = SykAlleDager()),
                ),
        ).runWithApplicationTestBuilder { scenarioData ->
            val forsteOppdrag = scenarioData.utbetalingsberegning!!.beregningData.spilleromOppdrag
            val personId = scenarioData.scenario.pseudoId

            val revurderendePeriode = revurder(personId, scenarioData.behandling.id)
            revurderendePeriode.revurdererSaksbehandlingsperiodeId `should equal` scenarioData.behandling.id

            forsteOppdrag.oppdrag.size `should equal` 1
            val førsteOppdragslinje = forsteOppdrag.oppdrag[0]
            førsteOppdragslinje.totalbeløp `should equal` 4620

            val yrkesaktivitet =
                hentYrkesaktiviteter(
                    behandlingId = revurderendePeriode.id,
                    personPseudoId = personId,
                ).first()

            settDagoversikt(
                personId = personId,
                behandlingId = revurderendePeriode.id,
                yrkesaktivitetId = yrkesaktivitet.id,
                dager = lagSykedager(fom = revurderendePeriode.fom, tom = revurderendePeriode.tom, grad = 50),
            )
            sendTilBeslutningOgForventOk(
                behandlingId = revurderendePeriode.id,
                personId = personId,
                individuellBegrunnelse = "Revurdering med lavere grad",
            )
            taTilBeslutningOgForventOk(personId, revurderendePeriode.id, token = scenarioData.beslutterToken)
            godkjennOgForventOk(personId, revurderendePeriode.id, token = scenarioData.beslutterToken)

            // Hent utbetalingsberegninger via API
            val opprinneligUtbetaling = scenarioData.utbetalingsberegning.beregningData.spilleromOppdrag
            val revurderendeUtbetaling = hentUtbetalingsberegning(personId, revurderendePeriode.id)!!.beregningData.spilleromOppdrag

            opprinneligUtbetaling.oppdrag.first().totalbeløp `should equal` 4620
            revurderendeUtbetaling.oppdrag.first().totalbeløp `should equal` 2310

            opprinneligUtbetaling.spilleromUtbetalingId `should equal` revurderendeUtbetaling.spilleromUtbetalingId

            hentAlleBehandlinger(personId).sortedByDescending { it.opprettet }.also {
                it.size `should equal` 2
                it.last().status `should equal` TidslinjeBehandlingStatus.REVURDERT
                it.last().revurdertAvBehandlingId `should equal` it.first().id
                it.first().status `should equal` TidslinjeBehandlingStatus.GODKJENT
                it.first().revurdererSaksbehandlingsperiodeId `should equal` it.last().id
            }
        }
    }
}
