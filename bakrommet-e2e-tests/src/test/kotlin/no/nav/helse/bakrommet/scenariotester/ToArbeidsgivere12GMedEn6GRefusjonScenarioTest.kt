package no.nav.helse.bakrommet.scenariotester

import no.nav.helse.Grunnbeløp
import no.nav.helse.bakrommet.api.dto.yrkesaktivitet.RefusjonsperiodeDto
import no.nav.helse.bakrommet.testutils.*
import kotlin.test.Test

class ToArbeidsgivere12GMedEn6GRefusjonScenarioTest {
    @Test
    fun `to arbeidsgivere med 6g hvor den ene refunderer 6g`() {
        val seksG = Grunnbeløp.`6G`.beløp(ScenarioDefaults.fom)

        Scenario(
            yrkesaktiviteter =
                listOf(
                    Arbeidstaker(
                        "988888888",
                        inntekt =
                            Inntektsmelding(
                                beregnetInntekt = seksG.månedlig,
                                refusjon = RefusjonsperiodeDto(ScenarioDefaults.fom, null, seksG.dtoMånedligDouble().beløp),
                            ),
                        dagoversikt = SykAlleDager(),
                    ),
                    Arbeidstaker("999999999", inntekt = Inntektsmelding(seksG.månedlig), dagoversikt = SykAlleDager()),
                ),
        ).run {
            `skal ha sykepengegrunnlag`(744168.0)
            `skal ha refusjon`(28620, orgnummer = "988888888")
            `skal ha refusjon`(0, orgnummer = "999999999")
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
                        "988888888",
                        inntekt =
                            Inntektsmelding(
                                seksG.månedlig,
                                emptyList(),
                                RefusjonsperiodeDto(ScenarioDefaults.fom, null, seksG.times(1.5).dtoMånedligDouble().beløp),
                            ),
                        dagoversikt = SykAlleDager(),
                    ),
                    Arbeidstaker("999999999", inntekt = Inntektsmelding(seksG.månedlig), dagoversikt = SykAlleDager()),
                ),
        ).run {
            `skal ha sykepengegrunnlag`(744168.0)
            `skal ha refusjon`(28620, orgnummer = "988888888")
            `skal ha refusjon`(0, orgnummer = "999999999")
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
                        "988888888",
                        inntekt =
                            Inntektsmelding(
                                seksG.månedlig,
                                emptyList(),
                                RefusjonsperiodeDto(ScenarioDefaults.fom, null, seksG.dtoMånedligDouble().beløp),
                            ),
                        dagoversikt = SykAlleDager(),
                    ),
                    Arbeidstaker(
                        "999999999",
                        inntekt =
                            Inntektsmelding(
                                seksG.månedlig,
                                emptyList(),
                                RefusjonsperiodeDto(ScenarioDefaults.fom, null, seksG.div(2).dtoMånedligDouble().beløp),
                            ),
                        dagoversikt = SykAlleDager(),
                    ),
                ),
        ).run {
            `skal ha sykepengegrunnlag`(744168.0)
            `skal ha refusjon`(19080, orgnummer = "988888888")
            `skal ha refusjon`(9540, orgnummer = "999999999")
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
                        "988888888",
                        inntekt =
                            Inntektsmelding(
                                seksG.månedlig,
                                emptyList(),
                                RefusjonsperiodeDto(ScenarioDefaults.fom, null, seksG.times(1.5).dtoMånedligDouble().beløp),
                            ),
                        dagoversikt = SykAlleDager(),
                    ),
                    Arbeidstaker(
                        "999999999",
                        inntekt =
                            Inntektsmelding(
                                seksG.månedlig,
                                emptyList(),
                                RefusjonsperiodeDto(ScenarioDefaults.fom, null, seksG.div(2).dtoMånedligDouble().beløp),
                            ),
                        dagoversikt = SykAlleDager(),
                    ),
                ),
        ).run {
            `skal ha sykepengegrunnlag`(744168.0)
            `skal ha refusjon`(19080, orgnummer = "988888888")
            `skal ha refusjon`(9540, orgnummer = "999999999")
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
                        "988888888",
                        inntekt =
                            Inntektsmelding(
                                seksG.månedlig.times(1.5),
                                emptyList(),
                                RefusjonsperiodeDto(ScenarioDefaults.fom, null, seksG.times(1.5).dtoMånedligDouble().beløp),
                            ),
                        dagoversikt = SykAlleDager(),
                    ),
                    Arbeidstaker(
                        "999999999",
                        inntekt =
                            Inntektsmelding(
                                seksG.månedlig,
                                emptyList(),
                                RefusjonsperiodeDto(ScenarioDefaults.fom, null, seksG.div(2).dtoMånedligDouble().beløp),
                            ),
                        dagoversikt = SykAlleDager(),
                    ),
                ),
        ).run {
            `skal ha sykepengegrunnlag`(744168.0)
            `skal ha refusjon`(21470, orgnummer = "988888888")
            `skal ha refusjon`(7160, orgnummer = "999999999")
            `skal ha direkteutbetaling`(0)
        }
    }
}
