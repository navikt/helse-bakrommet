package no.nav.helse.bakrommet.e2e.scenariotester

import no.nav.helse.bakrommet.e2e.testutils.AInntekt
import no.nav.helse.bakrommet.e2e.testutils.Arbeidstaker
import no.nav.helse.bakrommet.e2e.testutils.Scenario
import no.nav.helse.bakrommet.e2e.testutils.SykAlleDager
import no.nav.helse.bakrommet.e2e.testutils.saksbehandlerhandlinger.hentSykepengegrunnlag
import no.nav.helse.bakrommet.e2e.testutils.`should equal`
import kotlin.test.Test

class SykepengegrunnlagLåsesTest {
    @Test
    fun `sykepengegrunnlag i databasen låses når perioden er blitt godkjent`() {
        Scenario(
            yrkesaktiviteter =
                listOf(
                    Arbeidstaker("988888888", inntekt = AInntekt(10000, 10000, 10000), dagoversikt = SykAlleDager()),
                ),
        ).runWithApplicationTestBuilder {
            val spg = hentSykepengegrunnlag(it.scenario.pseudoId, it.behandling.id)
            spg!!.låst `should equal` true
        }
    }
}
