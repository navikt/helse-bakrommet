package no.nav.helse.bakrommet.behandling

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class StatusGuardTest {
    @Test
    fun `status-konstant som brukes i WHERE-guards skal ha riktig verdi`() {
        assertEquals(BehandlingStatus.UNDER_BEHANDLING.name, STATUS_UNDER_BEHANDLING_STR)
    }
}
