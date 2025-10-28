package no.nav.helse.bakrommet.scenariotester

import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.SelvstendigForsikring
import no.nav.helse.bakrommet.testutils.*
import kotlin.test.Test

class KombinertNæringsdrivendeArbeidstakerTest {
    @Test
    fun `kombinert selvstendig næringsdrivende og arbeidstaker`() {
        Scenario(
            listOf(
                Selvstendig(
                    forsikring = SelvstendigForsikring.FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG,
                    inntekt = SigrunInntekt(700000, 900000, 1000000),
                    dagoversikt = SykAlleDager(),
                ),
                Arbeidstaker("888", inntekt = AInntekt(10000, 10000, 10000), dagoversikt = SykAlleDager()),
            ),
        ).run {
            `skal ha sykepengegrunnlag`(744168.0)
            `skal ha utbetaling`(28630)
            `skal ha nærings del`(744168.0 - (10000.0 * 12))
        }
    }
}
