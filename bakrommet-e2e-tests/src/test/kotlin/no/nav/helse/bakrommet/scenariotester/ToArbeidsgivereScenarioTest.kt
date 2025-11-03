package no.nav.helse.bakrommet.scenariotester

import no.nav.helse.bakrommet.BeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_HOVEDREGEL
import no.nav.helse.bakrommet.BeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_SKJOENN_URIKTIG
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.Refusjonsperiode
import no.nav.helse.bakrommet.testutils.*
import no.nav.helse.dto.InntektbeløpDto
import kotlin.test.Test

class ToArbeidsgivereScenarioTest {
    @Test
    fun `to arbeidsgivere med ainntekt inntekt`() {
        Scenario(
            yrkesaktiviteter =
                listOf(
                    Arbeidstaker("888", inntekt = AInntekt(10000, 10000, 10000), dagoversikt = SykAlleDager()),
                    Arbeidstaker("999", inntekt = AInntekt(15000, 15000, 15000), dagoversikt = SykAlleDager()),
                ),
        ).run {
            `skal ha sykepengegrunnlag`(300000.0)
            `arbeidstaker yrkesaktivitet`(orgnummer = "888") harBeregningskode (ARBEIDSTAKER_SYKEPENGEGRUNNLAG_HOVEDREGEL)
            `arbeidstaker yrkesaktivitet`(orgnummer = "999") harBeregningskode (ARBEIDSTAKER_SYKEPENGEGRUNNLAG_HOVEDREGEL)
            `skal ha refusjon`(0, orgnummer = "888")
            `skal ha refusjon`(0, orgnummer = "999")
            `skal ha utbetaling`(11540)
        }
    }

    @Test
    fun `to arbeidsgivere med en ainntekt inntekt og en inntektsmelding med refusjon`() {
        Scenario(
            listOf(
                Arbeidstaker("888", inntekt = AInntekt(10000, 10000, 10000), dagoversikt = SykAlleDager()),
                Arbeidstaker(
                    "999",
                    inntekt =
                        Inntektsmelding(
                            15000.0,
                            Refusjonsperiode(
                                ScenarioDefaults.fom,
                                ScenarioDefaults.tom,
                                InntektbeløpDto.MånedligDouble(15000.0),
                            ),
                        ),
                    dagoversikt = SykAlleDager(),
                ),
            ),
        ).run {
            `skal ha sykepengegrunnlag`(300000.0)
            `arbeidstaker yrkesaktivitet`(orgnummer = "888") harBeregningskode (ARBEIDSTAKER_SYKEPENGEGRUNNLAG_HOVEDREGEL)
            `arbeidstaker yrkesaktivitet`(orgnummer = "999") harBeregningskode (ARBEIDSTAKER_SYKEPENGEGRUNNLAG_HOVEDREGEL)
            `skal ha refusjon`(0, orgnummer = "888")
            `skal ha refusjon`(6920, orgnummer = "999")
            `skal ha utbetaling`(4620)
        }
    }

    @Test
    fun `to arbeidsgivere med en skjønnsfastasatt grunnet manglende rapportering og en ainntekt`() {
        Scenario(
            yrkesaktiviteter =
                listOf(
                    Arbeidstaker("999", inntekt = AInntekt(10000, 10000, 10000), dagoversikt = SykAlleDager()),
                    Arbeidstaker(
                        "888",
                        inntekt = SkjønnsfastsattManglendeRapportering(15000.0 * 12),
                        dagoversikt = SykAlleDager(),
                    ),
                ),
        ).run {
            `skal ha sykepengegrunnlag`(300000.0)
            `arbeidstaker yrkesaktivitet`(orgnummer = "999") harBeregningskode (ARBEIDSTAKER_SYKEPENGEGRUNNLAG_HOVEDREGEL)
            `arbeidstaker yrkesaktivitet`(orgnummer = "888") harBeregningskode (ARBEIDSTAKER_SYKEPENGEGRUNNLAG_SKJOENN_URIKTIG)
        }
    }
}
