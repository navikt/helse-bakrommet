package no.nav.helse.bakrommet.scenariotester

import no.nav.helse.bakrommet.testutils.*
import kotlin.test.Test

class ToArbeidsgivereScenarioTest {
    @Test
    fun `to arbeidsgivere med ainntekt inntekt`() {
        Scenario(
            yrkesaktiviteter =
                listOf(
                    YA(YAType.ARBTAKER, "888", inntekt = AInntekt(10000, 10000, 10000), dagoversikt = SykAlleDager()),
                    YA(YAType.ARBTAKER, "999", inntekt = AInntekt(15000, 15000, 15000), dagoversikt = SykAlleDager()),
                ),
        ).run {
            it.apply {
                `skal ha sykepengegrunnlag`(300000.0)
                `arbeidstaker yrkesaktivitet`(orgnummer = "888") harBeregningskode ("ARB_SPG_HOVEDREGEL")
                `arbeidstaker yrkesaktivitet`(orgnummer = "999") harBeregningskode ("ARB_SPG_HOVEDREGEL")
            }
        }
    }

    @Test
    fun `to arbeidsgivere med en ainntekt inntekt og en inntektsmelding`() {
        Scenario(
            yrkesaktiviteter =
                listOf(
                    YA(YAType.ARBTAKER, "888", inntekt = AInntekt(10000, 10000, 10000), dagoversikt = SykAlleDager()),
                    YA(YAType.ARBTAKER, "999", inntekt = Inntektsmelding(15000.0), dagoversikt = SykAlleDager()),
                ),
        ).run {
            it.apply {
                `skal ha sykepengegrunnlag`(300000.0)
                `arbeidstaker yrkesaktivitet`(orgnummer = "888") harBeregningskode ("ARB_SPG_HOVEDREGEL")
                `arbeidstaker yrkesaktivitet`(orgnummer = "999") harBeregningskode ("ARB_SPG_HOVEDREGEL")
            }
        }
    }

    @Test
    fun `to arbeidsgivere med en skjønnsfastasatt grunnet manglende rapportering og en ainntekt`() {
        Scenario(
            yrkesaktiviteter =
                listOf(
                    YA(YAType.ARBTAKER, "999", inntekt = AInntekt(10000, 10000, 10000), dagoversikt = SykAlleDager()),
                    YA(
                        YAType.ARBTAKER,
                        "888",
                        inntekt = SkjønnsfastsattManglendeRapportering(15000.0 * 12),
                        dagoversikt = SykAlleDager(),
                    ),
                ),
        ).run {
            it.apply {
                `skal ha sykepengegrunnlag`(300000.0)
                `arbeidstaker yrkesaktivitet`(orgnummer = "999") harBeregningskode ("ARB_SPG_HOVEDREGEL")
                `arbeidstaker yrkesaktivitet`(orgnummer = "888") harBeregningskode ("SKJØNNSFASTSATT_MANGELFULL_RAPPORTERING TODO")
            }
        }
    }
}
