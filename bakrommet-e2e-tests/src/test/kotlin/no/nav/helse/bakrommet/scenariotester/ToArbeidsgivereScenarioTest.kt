package no.nav.helse.bakrommet.scenariotester

import no.nav.helse.bakrommet.testutils.*
import kotlin.test.Test

class ToArbeidsgivereScenarioTest {
    @Test
    fun `toArbeidsgivere scenario`() {
        Scenario(
            yrkesaktiviteter =
                listOf(
                    YA(YAType.ARBTAKER, "1", inntekt = AInntekt(10000, 10000, 10000), dagoversikt = SykAlleDager()),
                    YA(YAType.ARBTAKER, "2", inntekt = AInntekt(15000, 15000, 15000), dagoversikt = SykAlleDager()),
                ),
        ).run { _, resultat ->
            resultat `skal ha sykepengegrunnlag` 300000.0
        }
    }
}
