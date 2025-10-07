package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.Inntektskilde
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
                    fra(1.januar(2024))
                    til(31.januar(2024))
                }

                yrkesaktivitet {
                    id(yrkesaktivitetIdNæring)
                    næringsdrivende("FORSIKRING_80_PROSENT_FRA_FØRSTE_SYKEDAG")
                    fra(1.januar(2024))
                    syk(grad = 100, antallDager = 31)
                }

                yrkesaktivitet {
                    id(yrkesaktivitetIdArbeidstaker)
                    arbeidstaker("999999999")
                    fra(1.januar(2024))
                    syk(grad = 100, antallDager = 31)
                }

                inntekt {
                    yrkesaktivitetId(yrkesaktivitetIdNæring)
                    beløp(21666)
                    kilde(Inntektskilde.PENSJONSGIVENDE_INNTEKT)
                }
                inntekt {
                    yrkesaktivitetId(yrkesaktivitetIdArbeidstaker)
                    beløp(21666)
                    kilde(Inntektskilde.INNTEKTSMELDING)
                    refusjon {
                        fra(1.januar(2024))
                        til(31.januar(2024))
                        beløp(21667) // Full refusjon
                    }
                }
            }

        resultat.skal {
            haYrkesaktivitet(yrkesaktivitetIdNæring) {
                harAntallDager(31) // Hele januar
                harDekningsgrad(80)
                harDekningsgradBegrunnelse(ORDINAER_SELVSTENDIG_80)
                dag(1.januar(2024)) {
                    harGrad(100)
                    harIngenRefusjon()
                    harUtbetaling(800)
                }
            }

            haYrkesaktivitet(yrkesaktivitetIdArbeidstaker) {
                harAntallDager(31) // Hele januar
                harDekningsgrad(100)
                harDekningsgradBegrunnelse(ARBEIDSTAKER_100)
                dag(1.januar(2024)) {
                    harGrad(100)
                    harIngenUtbetaling()
                    harRefusjon(1000)
                }
            }

            haOppdrag {
                harAntallOppdrag(2) // Refusjon og person
                oppdrag(0) {
                    harFagområde("SP")
                    harNettoBeløp(18400)
                    harTotalbeløp(18400)
                }
                oppdrag(1) {
                    harFagområde("SPREF")
                    harNettoBeløp(23000)
                    harTotalbeløp(23000)
                }
            }
        }
    }
}
