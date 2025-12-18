package no.nav.helse.bakrommet.scenariotester

import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.DagDto
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.DagtypeDto
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.KildeDto
import no.nav.helse.bakrommet.testutils.Arbeidstaker
import no.nav.helse.bakrommet.testutils.GradertSyk
import no.nav.helse.bakrommet.testutils.Inntektsmelding
import no.nav.helse.bakrommet.testutils.Scenario
import no.nav.helse.bakrommet.testutils.direkteTotalbeløp
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.hentUtbetalingsberegning
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.hentYrkesaktiviteter
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.settDagoversikt
import no.nav.helse.bakrommet.testutils.`should equal`
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AvslåDagOgBeregningTest {
    @Test
    fun `Vi kan avslå og fjerne avslagsdag`() {
        Scenario(
            besluttOgGodkjenn = false,
            fom = LocalDate.of(2025, 1, 1),
            tom = LocalDate.of(2025, 1, 3),
            yrkesaktiviteter =
                listOf(
                    Arbeidstaker(
                        "988888888",
                        inntekt = Inntektsmelding(520000.0 / 12),
                        dagoversikt = GradertSyk(50),
                    ),
                ),
        ).runWithApplicationTestBuilder { førsteBehandling ->

            førsteBehandling.sykepengegrunnlag!!.sykepengegrunnlag `should equal` 519999.96
            hentUtbetalingsberegning(
                førsteBehandling.scenario.pseudoId,
                førsteBehandling.behandling.id,
            ).direkteTotalbeløp() `should equal` 3000
            hentYrkesaktiviteter(
                førsteBehandling.scenario.pseudoId,
                førsteBehandling.behandling.id,
            ).first().dagoversikt!!.also {
                it[0].dagtype `should equal` DagtypeDto.Syk
                it[1].dagtype `should equal` DagtypeDto.Syk
                it[2].dagtype `should equal` DagtypeDto.Syk
            }

            settDagoversikt(
                førsteBehandling.scenario.pseudoId,
                førsteBehandling.behandling.id,
                førsteBehandling.yrkesaktiviteter.first().id,
                listOf(
                    DagDto(
                        dato = LocalDate.of(2025, 1, 2),
                        dagtype = DagtypeDto.Avslått,
                        avslåttBegrunnelse = listOf("Godt begrunnet"),
                        grad = null,
                        kilde = KildeDto.Saksbehandler,
                    ),
                ),
            )

            settDagoversikt(
                førsteBehandling.scenario.pseudoId,
                førsteBehandling.behandling.id,
                førsteBehandling.yrkesaktiviteter.first().id,
                listOf(
                    DagDto(
                        dato = LocalDate.of(2025, 1, 1),
                        dagtype = DagtypeDto.Syk,
                        avslåttBegrunnelse = null,
                        grad = 20,
                        kilde = KildeDto.Saksbehandler,
                    ),
                ),
            )

            hentUtbetalingsberegning(
                førsteBehandling.scenario.pseudoId,
                førsteBehandling.behandling.id,
            ).direkteTotalbeløp() `should equal` 1400

            hentYrkesaktiviteter(
                førsteBehandling.scenario.pseudoId,
                førsteBehandling.behandling.id,
            ).first().dagoversikt!!.also {
                it[0].dagtype `should equal` DagtypeDto.Syk
                it[1].dagtype `should equal` DagtypeDto.Avslått
                it[2].dagtype `should equal` DagtypeDto.Syk
            }

            settDagoversikt(
                førsteBehandling.scenario.pseudoId,
                førsteBehandling.behandling.id,
                førsteBehandling.yrkesaktiviteter.first().id,
                listOf(
                    DagDto(
                        dato = LocalDate.of(2025, 1, 2),
                        dagtype = DagtypeDto.Syk,
                        avslåttBegrunnelse = null,
                        grad = 30,
                        kilde = KildeDto.Saksbehandler,
                    ),
                ),
            )

            hentUtbetalingsberegning(
                førsteBehandling.scenario.pseudoId,
                førsteBehandling.behandling.id,
            ).direkteTotalbeløp() `should equal` 2000

            hentYrkesaktiviteter(
                førsteBehandling.scenario.pseudoId,
                førsteBehandling.behandling.id,
            ).first().dagoversikt!!.also {
                it[0].dagtype `should equal` DagtypeDto.Syk
                it[1].dagtype `should equal` DagtypeDto.Syk
                it[2].dagtype `should equal` DagtypeDto.Syk
            }
        }
    }
}
