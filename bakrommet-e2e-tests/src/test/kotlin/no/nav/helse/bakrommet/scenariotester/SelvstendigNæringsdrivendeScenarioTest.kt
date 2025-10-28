package no.nav.helse.bakrommet.scenariotester

import no.nav.helse.bakrommet.testutils.*
import kotlin.test.Test

class SelvstendigNæringsdrivendeScenarioTest {
    @Test
    fun `enkel selvstendig næringsdrivende`() {
        Scenario(
            listOf(
                Selvstendig(inntekt = SigrunInntekt(700000, 900000, 1000000), dagoversikt = SykAlleDager()),
            ),
        ).run {
            it.apply {
                `skal ha sykepengegrunnlag`(744168.0)
                `skal ha utbetaling`(22900)
            }
        }
    }

    @Test
    fun `enkel selvstendig næringsdrivende alle år under 6g`() {
        Scenario(
            listOf(
                Selvstendig(inntekt = SigrunInntekt(400000, 400000, 400000), dagoversikt = SykAlleDager()),
            ),
        ).run {
            it.apply {
                `skal ha sykepengegrunnlag`(462094.0)
                `skal ha utbetaling`(14220)
            }
        }
    }

    @Test
    fun `enkel selvstendig næringsdrivende alle år under 6g 4 år rapportert`() {
        Scenario(
            listOf(
                Selvstendig(inntekt = SigrunInntekt(400000, 400000, 400000, 400000), dagoversikt = SykAlleDager()),
            ),
        ).run {
            it.apply {
                `skal ha sykepengegrunnlag`(462094.0)
                `skal ha utbetaling`(14220)
            }
        }
    }

    @Test
    fun `Fjoråret er ikke liknet, men har 3 år før det`() {
        Scenario(
            listOf(
                Selvstendig(inntekt = SigrunInntekt(400000, 400000, 400000, null), dagoversikt = SykAlleDager()),
            ),
        ).run {
            it.apply {
                `skal ha sykepengegrunnlag`(484340.0)
                `skal ha utbetaling`(14900)
            }
        }
    }

    @Test
    fun `enkel selvstendig næringsdrivende alle år under 6g 4 år rapportert, 0 for 2 år siden`() {
        Scenario(
            listOf(
                Selvstendig(inntekt = SigrunInntekt(400000, 400000, 0, 400000), dagoversikt = SykAlleDager()),
            ),
        ).run {
            it.apply {
                `skal ha sykepengegrunnlag`(311461.0)
                `skal ha utbetaling`(9580)
            }
        }
    }


}
