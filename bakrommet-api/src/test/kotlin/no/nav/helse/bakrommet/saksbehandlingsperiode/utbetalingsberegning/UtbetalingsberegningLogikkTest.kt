package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.Inntektskilde
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class UtbetalingsberegningLogikkTest {
    @Test
    fun `beregner utbetaling med åpen refusjonsperiode`() {
        val yrkesaktivitetId = UUID.randomUUID()

        val resultat =
            utbetalingsberegningTestOgBeregn {
                periode {
                    fra(LocalDate.of(2024, 1, 1))
                    til(LocalDate.of(2024, 1, 31))
                }

                yrkesaktivitet {
                    id(yrkesaktivitetId)
                    arbeidstaker()
                    this.fra(LocalDate.of(2024, 1, 1))
                    syk(grad = 100, antallDager = 2)
                }

                inntekt {
                    yrkesaktivitetId(yrkesaktivitetId)
                    beløp(50000) // 50 000 kr/mnd
                    kilde(Inntektskilde.AINNTEKT)
                    refusjon {
                        fra(LocalDate.of(2024, 1, 1))
                        åpen()
                        beløp(10000) // 10 000 kr/mnd refusjon
                    }
                }
            }

        resultat.skal {
            haYrkesaktivitet(yrkesaktivitetId) {
                harAntallDager(31) // Hele januar
                dag(LocalDate.of(2024, 1, 1)) {
                    harGrad(100)
                    harRefusjon()
                }
                dag(LocalDate.of(2024, 1, 2)) {
                    harGrad(100)
                    harRefusjon()
                }
            }
        }
    }

    @Test
    fun `beregner utbetaling med blandet refusjon (lukket og åpen)`() {
        val yrkesaktivitetId = UUID.randomUUID()

        val resultat =
            utbetalingsberegningTestOgBeregn {
                periode {
                    fra(LocalDate.of(2024, 1, 1))
                    til(LocalDate.of(2024, 3, 31))
                }

                yrkesaktivitet {
                    id(yrkesaktivitetId)
                    arbeidstaker()
                    this.fra(LocalDate.of(2024, 1, 10))
                    syk(grad = 100, antallDager = 1)
                    this.fra(LocalDate.of(2024, 2, 10))
                    syk(grad = 100, antallDager = 1)
                    this.fra(LocalDate.of(2024, 3, 10))
                    syk(grad = 100, antallDager = 1)
                }

                inntekt {
                    yrkesaktivitetId(yrkesaktivitetId)
                    beløp(50000) // 50 000 kr/mnd
                    kilde(Inntektskilde.AINNTEKT)
                    refusjon {
                        fra(LocalDate.of(2024, 1, 1))
                        til(LocalDate.of(2024, 1, 15))
                        beløp(10000) // 10 000 kr/mnd refusjon
                    }
                    refusjon {
                        fra(LocalDate.of(2024, 2, 1))
                        åpen()
                        beløp(20000) // 20 000 kr/mnd refusjon
                    }
                }
            }

        resultat.skal {
            haYrkesaktivitet(yrkesaktivitetId) {
                harAntallDager(91) // Jan-mars 2024
                dag(LocalDate.of(2024, 1, 10)) {
                    harRefusjon() // Dag i lukket refusjonsperiode skal ha refusjon
                }
            }
        }
    }
}
