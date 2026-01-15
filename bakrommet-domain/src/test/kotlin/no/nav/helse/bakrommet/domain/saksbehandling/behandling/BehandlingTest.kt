package no.nav.helse.bakrommet.domain.saksbehandling.behandling

import no.nav.helse.bakrommet.domain.enBehandling
import no.nav.helse.bakrommet.domain.enNaturligIdent
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BehandlingTest {
    @ParameterizedTest
    @EnumSource(BehandlingStatus::class, names = ["UNDER_BEHANDLING"], mode = EnumSource.Mode.EXCLUDE)
    fun `ikke under behandling`(status: BehandlingStatus) {
        val behandling = enBehandling(status = status)
        assertFalse(behandling.erÅpenForEndringer())
    }

    @Test
    fun `under behandling`() {
        val behandling = enBehandling(status = BehandlingStatus.UNDER_BEHANDLING)
        assertTrue(behandling.erÅpenForEndringer())
    }

    @Test
    fun `gjelder naturlig ident`() {
        val behandling = enBehandling(status = BehandlingStatus.UNDER_BEHANDLING)
        assertTrue(behandling.gjelder(behandling.naturligIdent))
    }

    @Test
    fun `gjelder ikke naturlig ident`() {
        val behandling = enBehandling(status = BehandlingStatus.UNDER_BEHANDLING)
        assertFalse(behandling.gjelder(enNaturligIdent()))
    }
}
