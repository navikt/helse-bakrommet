package no.nav.helse.bakrommet.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BrukerTest {
    @Test
    fun `er tildelt`() {
        val bruker = enBruker()
        val behandling = enBehandling(opprettetAvNavIdent = bruker.navIdent)

        assertTrue(bruker.erTildelt(behandling))
    }

    @Test
    fun `er ikke tildelt`() {
        val bruker = enBruker()
        val behandling = enBehandling(opprettetAvNavIdent = enNavIdent())

        assertFalse(bruker.erTildelt(behandling))
    }
}
