package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.Inntektskilde
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.Beregningssporing.DAGPENGEMOTTAKER_100
import no.nav.helse.januar
import org.junit.jupiter.api.Test
import java.util.UUID

class ArbeidsledigBeregningTest {
    @Test
    fun `beregner utbetaling for arbeidsledig`() {
        val yrkesaktivitetId = UUID.randomUUID()

        val resultat =
            utbetalingsberegningTestOgBeregn {
                periode {
                    fra(1.januar(2024))
                    til(31.januar(2024))
                }

                yrkesaktivitet {
                    id(yrkesaktivitetId)
                    arbeidsledig()
                    fra(1.januar(2024))
                    syk(grad = 50, antallDager = 15)
                    syk(grad = 100, antallDager = 16)
                }

                inntekt {
                    yrkesaktivitetId(yrkesaktivitetId)
                    beløp(21666)
                    kilde(Inntektskilde.AINNTEKT)
                }
            }

        resultat.skal {
            haYrkesaktivitet(yrkesaktivitetId) {
                harAntallDager(31) // Hele januar
                harDekningsgrad(100)
                harDekningsgradBegrunnelse(DAGPENGEMOTTAKER_100)
                dag(1.januar(2024)) {
                    harGrad(50)
                    harIngenRefusjon()
                    harUtbetaling(500)
                }
                dag(16.januar(2024)) {
                    harGrad(100)
                    harIngenRefusjon()
                    harUtbetaling(1000)
                }
            }

            haOppdrag {
                harAntallOppdrag(1) // Refusjon og person
                oppdrag {
                    harFagområde("SP")
                    harNettoBeløp(17500)
                    harTotalbeløp(17500)
                }
            }
        }
    }
}
