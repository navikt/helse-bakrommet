package no.nav.helse.bakrommet.domain.saksbehandling.behandling

import no.nav.helse.bakrommet.domain.person.NaturligIdent
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BehandlingTest {
    @ParameterizedTest
    @EnumSource(BehandlingStatus::class, names = ["UNDER_BEHANDLING"], mode = EnumSource.Mode.EXCLUDE)
    fun `ikke under behandling`(status: BehandlingStatus) {
        val behandling = behandlingMed(status)
        assertFalse(behandling.erÅpenForEndringer())
    }

    @Test
    fun `under behandling`() {
        val behandling = behandlingMed(BehandlingStatus.UNDER_BEHANDLING)
        assertTrue(behandling.erÅpenForEndringer())
    }

    private fun behandlingMed(status: BehandlingStatus): Behandling =
        Behandling.fraLagring(
            id = BehandlingId(UUID.randomUUID()),
            naturligIdent = NaturligIdent("12345678910"),
            opprettet = Instant.now(),
            opprettetAvNavIdent = "Z999999",
            opprettetAvNavn = "En Saksbehandler",
            fom = LocalDate.now(),
            tom = LocalDate.now(),
            status = status,
            beslutterNavIdent = "Z999999",
            skjæringstidspunkt = LocalDate.now(),
            individuellBegrunnelse = "En begrunnelse",
            sykepengegrunnlagId = null,
            revurdertAvBehandlingId = null,
            revurdererSaksbehandlingsperiodeId = null,
        )
}
