package no.nav.helse.bakrommet.behandling

import no.nav.helse.bakrommet.domain.enNaturligIdent
import no.nav.helse.bakrommet.runApplicationTest
import no.nav.helse.bakrommet.testutils.saksbehandlerhandlinger.*
import no.nav.helse.bakrommet.testutils.`should equal`
import no.nav.helse.bakrommet.testutils.tidsstuttet
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
                opprettBehandling(
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
            val perioder = hentAllePerioder(personPseudoId)
            perioder.size `should equal` 1
            perioder `should equal` listOf(behandling)
            println(perioder)
        }

    @Test
    fun `henter alle behandlinger uten filter eller paginering`() {
        runApplicationTest {
            val naturligIdent = enNaturligIdent()
            val personPseudoId = personsøk(naturligIdent)

            val naturligIdent2 = enNaturligIdent()
            val personPseudoId2 = personsøk(naturligIdent2)

            val behandling1 =
                opprettBehandling(
                    personPseudoId,
                    1.januar(2023),
                    31.januar(2023),
                )
            val behandling2 =
                opprettBehandling(
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
            val opprettetPeriode =
                opprettBehandling(
                    personPseudoId,
                    1.januar(2023),
                    31.januar(2023),
                )

            val nyttSkjæringstidspunkt = 15.januar(2023)
            val oppdatertPeriode = settSkjaeringstidspunkt(personPseudoId.toString(), opprettetPeriode.id, nyttSkjæringstidspunkt)

            assertEquals(nyttSkjæringstidspunkt, oppdatertPeriode.skjæringstidspunkt)
        }
}
