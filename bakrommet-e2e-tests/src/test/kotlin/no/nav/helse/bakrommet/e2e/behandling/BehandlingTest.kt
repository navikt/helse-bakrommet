package no.nav.helse.bakrommet.e2e.behandling

import no.nav.helse.bakrommet.domain.enNaturligIdent
import no.nav.helse.bakrommet.e2e.runApplicationTest
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.*
import no.nav.helse.bakrommet.e2e.testutils.`should equal`
import no.nav.helse.bakrommet.e2e.testutils.tidsstuttet
import no.nav.helse.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BehandlingTest {
    @Test
    fun `oppretter behandling`() =
        runApplicationTest {
            val naturligIdent = enNaturligIdent()
            val personPseudoId = personsøk(naturligIdent)

            val behandling =
                opprettBehandlingOgForventOk(
                    personPseudoId,
                    1.januar(2023),
                    31.januar(2023),
                )
            behandling.fom.toString() `should equal` "2023-01-01"
            behandling.tom.toString() `should equal` "2023-01-31"
            behandling.naturligIdent `should equal` naturligIdent.value
            behandling.opprettetAvNavIdent `should equal` "tullebruker"
            behandling.opprettetAvNavn `should equal` "Tulla Bruker"

            // Hent alle perioder via action
            val behandlinger = hentAlleBehandlinger(personPseudoId)
            behandlinger.size `should equal` 1
            behandlinger.tidsstuttet() `should equal` listOf(behandling).tidsstuttet()
            println(behandlinger)
        }

    @Test
    fun `henter alle behandlinger uten filter eller paginering`() {
        runApplicationTest {
            val naturligIdent = enNaturligIdent()
            val personPseudoId = personsøk(naturligIdent)

            val naturligIdent2 = enNaturligIdent()
            val personPseudoId2 = personsøk(naturligIdent2)

            val behandling1 =
                opprettBehandlingOgForventOk(
                    personPseudoId,
                    1.januar(2023),
                    31.januar(2023),
                )
            val behandling2 =
                opprettBehandlingOgForventOk(
                    personPseudoId2,
                    1.januar(2023),
                    31.januar(2023),
                )

            val alleBehandlinger = hentAlleBehandlinger()
            assertEquals(
                listOf(behandling1, behandling2).tidsstuttet().toSet(),
                alleBehandlinger.tidsstuttet().toSet(),
            )
        }
    }

    @Test
    fun `kan oppdatere skjæringstidspunkt`() =
        runApplicationTest {
            val naturligIdent = enNaturligIdent()
            val personPseudoId = personsøk(naturligIdent)
            val opprettetBehandling =
                opprettBehandlingOgForventOk(
                    personPseudoId,
                    1.januar(2023),
                    31.januar(2023),
                )

            val nyttSkjæringstidspunkt = 15.januar(2023)
            val oppdatertBehandling = settSkjaeringstidspunkt(personPseudoId.toString(), opprettetBehandling.id, nyttSkjæringstidspunkt)

            assertEquals(nyttSkjæringstidspunkt, oppdatertBehandling.skjæringstidspunkt)
        }
}
