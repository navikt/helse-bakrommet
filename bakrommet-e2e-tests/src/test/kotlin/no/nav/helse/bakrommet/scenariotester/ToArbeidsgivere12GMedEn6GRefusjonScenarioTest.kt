package no.nav.helse.bakrommet.scenariotester

import no.nav.helse.Grunnbeløp
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.Refusjonsperiode
import no.nav.helse.bakrommet.testutils.*
import kotlin.test.Test
import kotlin.time.times

class ToArbeidsgivere12GMedEn6GRefusjonScenarioTest {
    @Test
    fun `to arbeidsgivere med 6g hvor den ene refunderer 6g`() {
        val seksG = Grunnbeløp.`6G`.beløp(ScenarioDefaults.fom)

        Scenario(
            yrkesaktiviteter =
                listOf(
                    Arbeidstaker(
                        "888",
                        inntekt =
                            Inntektsmelding(
                                seksG.månedlig,
                                Refusjonsperiode(ScenarioDefaults.fom, null, seksG.dtoMånedligDouble()),
                            ),
                        dagoversikt = SykAlleDager(),
                    ),
                    Arbeidstaker("999", inntekt = Inntektsmelding(seksG.månedlig), dagoversikt = SykAlleDager()),
                ),
        ).run {
            `skal ha sykepengegrunnlag`(744168.0)
            `skal ha refusjon`(28620, orgnummer = "888")
            `skal ha refusjon`(0, orgnummer = "999")
            `skal ha direkteutbetaling`(0)
        }
    }

    @Test
    fun `to arbeidsgivere med 6g hvor den ene refunderer 9g`() {
        val seksG = Grunnbeløp.`6G`.beløp(ScenarioDefaults.fom)

        Scenario(
            yrkesaktiviteter =
                listOf(
                    Arbeidstaker(
                        "888",
                        inntekt =
                            Inntektsmelding(
                                seksG.månedlig,
                                Refusjonsperiode(ScenarioDefaults.fom, null, seksG.times(1.5).dtoMånedligDouble()),
                            ),
                        dagoversikt = SykAlleDager(),
                    ),
                    Arbeidstaker("999", inntekt = Inntektsmelding(seksG.månedlig), dagoversikt = SykAlleDager()),
                ),
        ).run {
            `skal ha sykepengegrunnlag`(744168.0)
            `skal ha refusjon`(28620, orgnummer = "888")
            `skal ha refusjon`(0, orgnummer = "999")
            `skal ha direkteutbetaling`(0)
        }
    }

    @Test
    fun `to arbeidsgivere med 6g hvor den ene refunderer 6g den andre 3g`() {
        val seksG = Grunnbeløp.`6G`.beløp(ScenarioDefaults.fom)

        Scenario(
            yrkesaktiviteter =
                listOf(
                    Arbeidstaker(
                        "888",
                        inntekt =
                            Inntektsmelding(
                                seksG.månedlig,
                                Refusjonsperiode(ScenarioDefaults.fom, null, seksG.dtoMånedligDouble()),
                            ),
                        dagoversikt = SykAlleDager(),
                    ),
                    Arbeidstaker(
                        "999",
                        inntekt =
                            Inntektsmelding(
                                seksG.månedlig,
                                Refusjonsperiode(ScenarioDefaults.fom, null, seksG.div(2).dtoMånedligDouble()),
                            ),
                        dagoversikt = SykAlleDager(),
                    ),
                ),
        ).run {
            `skal ha sykepengegrunnlag`(744168.0)
            `skal ha refusjon`(19080, orgnummer = "888")
            `skal ha refusjon`(9540, orgnummer = "999")
            `skal ha direkteutbetaling`(0)
        }
    }


    @Test
    fun `to arbeidsgivere med 6g hvor den ene refunderer 9g og den andre 3g`() {
        val seksG = Grunnbeløp.`6G`.beløp(ScenarioDefaults.fom)

        Scenario(
            yrkesaktiviteter =
                listOf(
                    Arbeidstaker(
                        "888",
                        inntekt =
                            Inntektsmelding(
                                seksG.månedlig,
                                Refusjonsperiode(ScenarioDefaults.fom, null, seksG.times(1.5).dtoMånedligDouble()),
                            ),
                        dagoversikt = SykAlleDager(),
                    ),
                    Arbeidstaker(
                        "999", inntekt = Inntektsmelding(
                            seksG.månedlig,
                            Refusjonsperiode(ScenarioDefaults.fom, null, seksG.div(2).dtoMånedligDouble()),
                        ), dagoversikt = SykAlleDager()
                    ),
                ),
        ).run {
            `skal ha sykepengegrunnlag`(744168.0)
            `skal ha refusjon`(19080, orgnummer = "888")
            `skal ha refusjon`(9540, orgnummer = "999")
            `skal ha direkteutbetaling`(0)
        }
    }


    @Test
    fun `to arbeidsgivere med 1 9g og en 6g hvor den ene refunderer 9g og den andre 3g`() {
        val seksG = Grunnbeløp.`6G`.beløp(ScenarioDefaults.fom)

        Scenario(
            yrkesaktiviteter =
                listOf(
                    Arbeidstaker(
                        "888",
                        inntekt =
                            Inntektsmelding(
                                seksG.månedlig.times(1.5),
                                Refusjonsperiode(ScenarioDefaults.fom, null, seksG.times(1.5).dtoMånedligDouble()),
                            ),
                        dagoversikt = SykAlleDager(),
                    ),
                    Arbeidstaker(
                        "999", inntekt = Inntektsmelding(
                            seksG.månedlig,
                            Refusjonsperiode(ScenarioDefaults.fom, null, seksG.div(2).dtoMånedligDouble()),
                        ), dagoversikt = SykAlleDager()
                    ),
                ),
        ).run {
            `skal ha sykepengegrunnlag`(744168.0)
            `skal ha refusjon`(21470, orgnummer = "888")
            `skal ha refusjon`(7160, orgnummer = "999")
            `skal ha direkteutbetaling`(0)
        }
    }
}
