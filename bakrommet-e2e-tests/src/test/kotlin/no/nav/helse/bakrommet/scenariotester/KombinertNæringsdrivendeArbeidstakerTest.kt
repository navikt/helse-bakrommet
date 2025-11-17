package no.nav.helse.bakrommet.scenariotester

import no.nav.helse.bakrommet.BeregningskoderSykepengegrunnlag.SELVSTENDIG_SYKEPENGEGRUNNLAG_HOVEDREGEL
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.SelvstendigForsikring
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
            `næringsdrivende yrkesaktivitet`().harBeregningskode(SELVSTENDIG_SYKEPENGEGRUNNLAG_HOVEDREGEL)
        }
    }

    @Test
    fun `kombinert selvstendig næringsdrivende og arbeidstaker med høy arbeidstakerinntekt`() {
        Scenario(
            listOf(
                Selvstendig(
                    forsikring = SelvstendigForsikring.FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG,
                    inntekt = SigrunInntekt(700000, 900000, 1000000),
                    dagoversikt = SykAlleDager(),
                ),
                Arbeidstaker("888", inntekt = AInntekt(70000, 70000, 70000), dagoversikt = SykAlleDager()),
            ),
        ).run {
            `skal ha sykepengegrunnlag`(744168.0)
            // 10 kr lavere enn scenario 1 pga. avrundingseffekt:
            // Når to yrkesaktiviteter deler på 6G (2400.646 + 461.538 = 2862.184) blir begge avrundet opp separat → 2863 kr/dag
            // Når én yrkesaktivitet får alt (2862.184) blir det avrundet ned → 2862 kr/dag
            // Differanse: 1 kr/dag × 10 dager = 10 kr
            `skal ha utbetaling`(28620)
            `skal ha nærings del`(0.0)
            `næringsdrivende yrkesaktivitet`().harBeregningskode(SELVSTENDIG_SYKEPENGEGRUNNLAG_HOVEDREGEL)
        }
    }
}
