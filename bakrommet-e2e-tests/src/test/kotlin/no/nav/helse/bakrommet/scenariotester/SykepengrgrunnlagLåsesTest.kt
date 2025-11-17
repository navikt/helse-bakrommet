package no.nav.helse.bakrommet.scenariotester

import no.nav.helse.bakrommet.testutils.*
import kotlin.test.Test

class SykepengrgrunnlagLåsesTest {
    @Test
    fun `sykepengegrunnlag i databasen låses når perioden er blitt godkjent`() {
        Scenario(
            yrkesaktiviteter =
                listOf(
                    Arbeidstaker("888", inntekt = AInntekt(10000, 10000, 10000), dagoversikt = SykAlleDager()),
                ),
        ).runWithApplicationTestBuilder {
            val sykepengegrunnlagId = it.periode.sykepengegrunnlagId!!

            val spgDbRec = it.daoer.sykepengegrunnlagDao.hentSykepengegrunnlag(sykepengegrunnlagId)
            spgDbRec.låst `should equal` true
        }
    }
}
