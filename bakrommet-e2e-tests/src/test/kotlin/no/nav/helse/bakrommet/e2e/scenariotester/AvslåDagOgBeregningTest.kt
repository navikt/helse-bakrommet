package no.nav.helse.bakrommet.e2e.scenariotester

import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.DagDto
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.DagtypeDto
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.KildeDto
import no.nav.helse.bakrommet.domain.etOrganisasjonsnummer
import no.nav.helse.bakrommet.e2e.testutils.Arbeidstaker
import no.nav.helse.bakrommet.e2e.testutils.GradertSyk
import no.nav.helse.bakrommet.e2e.testutils.Inntektsmelding
import no.nav.helse.bakrommet.e2e.testutils.Scenario
import no.nav.helse.bakrommet.e2e.testutils.direkteTotalbeløp
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.hentUtbetalingsberegning
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.hentYrkesaktiviteter
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.settDagoversikt
import no.nav.helse.bakrommet.e2e.testutils.`should equal`
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
                        etOrganisasjonsnummer(),
                        inntekt = Inntektsmelding(520000.0 / 12),
                        dagoversikt = GradertSyk(50),
                    ),
                ),
        ).runWithApplicationTestBuilder { førsteBehandling ->

            førsteBehandling.sykepengegrunnlag!!.sykepengegrunnlag `should equal` 519999.96
            hentUtbetalingsberegning(
                førsteBehandling.scenario.pseudoId,
                førsteBehandling.behandling.id,
            ).direkteTotalbeløp(førsteBehandling.scenario.fnr) `should equal` 3000
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
            ).direkteTotalbeløp(førsteBehandling.scenario.fnr) `should equal` 1400

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
            ).direkteTotalbeløp(førsteBehandling.scenario.fnr) `should equal` 2000

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
