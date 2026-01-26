package no.nav.helse.bakrommet.e2e.bruker

import no.nav.helse.bakrommet.domain.Rolle
import no.nav.helse.bakrommet.e2e.TestOppsett.oAuthMock
import no.nav.helse.bakrommet.e2e.runApplicationTest
import no.nav.helse.bakrommet.e2e.testutils.ApiResult
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.hentBruker
import no.nav.helse.bakrommet.e2e.testutils.`should contain`
import no.nav.helse.bakrommet.e2e.testutils.`should equal`
import org.junit.jupiter.api.Test

class BrukerRouteTest {
    @Test
    fun `les og beslutter`() {
        runApplicationTest {
            val result =
                hentBruker(
                    token = oAuthMock.token(grupper = listOf("GRUPPE_LES", "B", "C", "GRUPPE_BESLUTTER")),
                )

            check(result is ApiResult.Success) { "Henting av bruker feilet" }
            val bruker = result.response

            bruker.roller.`should contain`(Rolle.LES)
            bruker.roller.`should contain`(Rolle.BESLUTTER)
            bruker.roller.size `should equal` 2
        }
    }

    @Test
    fun `bare saksbehandler`() {
        runApplicationTest {
            val result =
                hentBruker(
                    token = oAuthMock.token(grupper = listOf("B", "C", "GRUPPE_SAKSBEHANDLER")),
                )

            check(result is ApiResult.Success) { "Henting av bruker feilet" }
            val bruker = result.response

            bruker.roller.`should contain`(Rolle.SAKSBEHANDLER)
            bruker.roller.size `should equal` 1
        }
    }

    @Test
    fun `bare les`() {
        runApplicationTest {
            val result =
                hentBruker(
                    token = oAuthMock.token(grupper = listOf("GRUPPE_LES", "B", "C")),
                )

            check(result is ApiResult.Success) { "Henting av bruker feilet" }
            val bruker = result.response

            bruker.roller.`should contain`(Rolle.LES)
            bruker.roller.size `should equal` 1
        }
    }
}
