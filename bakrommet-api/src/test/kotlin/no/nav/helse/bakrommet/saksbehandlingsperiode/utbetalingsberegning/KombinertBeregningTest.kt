package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlagold.Inntektskilde
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.Beregningssporing.ARBEIDSTAKER_100
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.Beregningssporing.ORDINAER_SELVSTENDIG_80
import no.nav.helse.januar
import org.junit.jupiter.api.Test
import java.util.UUID

class KombinertBeregningTest {
    @Test
    fun `beregner utbetaling for kombinert næringsdrivende med arbeidstaker som har refusjon`() {
        val yrkesaktivitetIdNæring = UUID.randomUUID()
        val yrkesaktivitetIdArbeidstaker = UUID.randomUUID()

        val resultat =
            utbetalingsberegningTestOgBeregn {
                periode {
                    `fra dato`(1.januar(2024))
                    `til dato`(31.januar(2024))
                }

                yrkesaktivitet {
                    id(yrkesaktivitetIdNæring)
                    `som næringsdrivende`("FORSIKRING_80_PROSENT_FRA_FØRSTE_SYKEDAG")
                    `fra dato`(1.januar(2024))
                    `er syk`(grad = 100, antallDager = 31)
                }

                yrkesaktivitet {
                    id(yrkesaktivitetIdArbeidstaker)
                    `som arbeidstaker`("999999999")
                    `fra dato`(1.januar(2024))
                    `er syk`(grad = 100, antallDager = 31)
                }

                inntekt {
                    yrkesaktivitetId(yrkesaktivitetIdNæring)
                    `med beløp`(21666)
                    `fra kilde`(Inntektskilde.PENSJONSGIVENDE_INNTEKT)
                }
                inntekt {
                    yrkesaktivitetId(yrkesaktivitetIdArbeidstaker)
                    `med beløp`(21666)
                    `fra kilde`(Inntektskilde.INNTEKTSMELDING)
                    `med refusjon` {
                        `fra dato`(1.januar(2024))
                        `til dato`(31.januar(2024))
                        `med beløp`(21667) // Full refusjon
                    }
                }
            }

        resultat.skal {
            `ha yrkesaktivitet`(yrkesaktivitetIdNæring) {
                `skal ha antall dager`(31) // Hele januar
                `skal ha dekningsgrad`(80)
                `skal ha dekningsgrad begrunnelse`(ORDINAER_SELVSTENDIG_80)
                `på dato`(1.januar(2024)) {
                    `skal ha total grad`(100)
                    `skal ha ingen refusjon`()
                    `skal ha utbetaling`(800)
                }
            }

            `ha yrkesaktivitet`(yrkesaktivitetIdArbeidstaker) {
                `skal ha antall dager`(31) // Hele januar
                `skal ha dekningsgrad`(100)
                `skal ha dekningsgrad begrunnelse`(ARBEIDSTAKER_100)
                `på dato`(1.januar(2024)) {
                    `skal ha total grad`(100)
                    `skal ha ingen utbetaling`()
                    `skal ha refusjon`(1000)
                }
            }

            `har oppdrag` {
                `skal ha antall oppdrag`(2) // Refusjon og person
                `oppdrag nummer`(0) {
                    `skal ha fagområde`("SP")
                    `skal ha netto beløp`(18400)
                    `skal ha total beløp`(18400)
                }
                `oppdrag nummer`(1) {
                    `skal ha fagområde`("SPREF")
                    `skal ha netto beløp`(23000)
                    `skal ha total beløp`(23000)
                }
            }
        }
    }
}
