package no.nav.helse.bakrommet.scenariotester

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.bakrommet.api.dto.tidslinje.TidslinjeBehandlingStatus
import no.nav.helse.bakrommet.kafka.OutboxDbRecord
import no.nav.helse.bakrommet.kafka.dto.oppdrag.SpilleromOppdragDto
import no.nav.helse.bakrommet.taTilBesluting
import no.nav.helse.bakrommet.testutils.*
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.godkjenn
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.hentAllePerioder
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.hentYrkesaktiviteter
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.revurder
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.sendTilBeslutning
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.settDagoversikt
import no.nav.helse.bakrommet.util.objectMapper
import org.junit.jupiter.api.Test

class RevurderingTest {
    @Test
    fun `Vi revurderer og endrer graderingen lavere`() {
        Scenario(
            yrkesaktiviteter =
                listOf(
                    Arbeidstaker("988888888", inntekt = AInntekt(10000, 10000, 10000), dagoversikt = SykAlleDager()),
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
                    pseudoID = personId,
                ).first()

            settDagoversikt(
                personId = personId,
                behandlingId = revurderendePeriode.id,
                yrkesaktivitetId = yrkesaktivitet.id,
                dager = lagSykedager(fom = revurderendePeriode.fom, tom = revurderendePeriode.tom, grad = 50),
            )
            sendTilBeslutning(
                behandlingId = revurderendePeriode.id,
                personId = personId,
                individuellBegrunnelse = "Revurdering med lavere grad",
            )
            taTilBesluting(personId, revurderendePeriode.id, token = scenarioData.beslutterToken)
            godkjenn(personId, revurderendePeriode.id, token = scenarioData.beslutterToken)

            val utbetalingKafkaMeldinger =
                scenarioData.daoer.outboxDao
                    .hentAlleUpubliserteEntries()
                    .filter { it.topic == "speilvendt.sykepenger-spillerom-utbetalinger" }
            utbetalingKafkaMeldinger.size `should equal` 2
            val opprinneligUtbetaling = utbetalingKafkaMeldinger[0].tilSpilleromOppdragDto()
            val revurderendeUtbetaling = utbetalingKafkaMeldinger[1].tilSpilleromOppdragDto()

            opprinneligUtbetaling.oppdrag.first().totalbeløp `should equal` 4620
            revurderendeUtbetaling.oppdrag.first().totalbeløp `should equal` 2310

            opprinneligUtbetaling.spilleromUtbetalingId `should equal` revurderendeUtbetaling.spilleromUtbetalingId

            hentAllePerioder(personId).also {
                it.size `should equal` 2
                it.last().status `should equal` TidslinjeBehandlingStatus.REVURDERT
                it.last().revurdertAvBehandlingId `should equal` it.first().id
                it.first().status `should equal` TidslinjeBehandlingStatus.GODKJENT
                it.first().revurdererSaksbehandlingsperiodeId `should equal` it.last().id
            }
        }
    }
}

fun OutboxDbRecord.tilSpilleromOppdragDto(): SpilleromOppdragDto = objectMapper.readValue(this.kafkaPayload)
