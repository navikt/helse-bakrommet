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
                    `fra dato`(LocalDate.of(2024, 1, 1))
                    `til dato`(LocalDate.of(2024, 1, 31))
                }

                yrkesaktivitet {
                    id(yrkesaktivitetId)
                    `som arbeidstaker`()
                    `fra dato`(LocalDate.of(2024, 1, 1))
                    `er syk`(grad = 100, antallDager = 2)
                }

                inntekt {
                    yrkesaktivitetId(yrkesaktivitetId)
                    `med beløp`(50000) // 50 000 kr/mnd
                    `fra kilde`(Inntektskilde.AINNTEKT)
                    `med refusjon` {
                        `fra dato`(LocalDate.of(2024, 1, 1))
                        `er åpen`()
                        `med beløp`(10000) // 10 000 kr/mnd refusjon
                    }
                }
            }

        resultat.skal {
            `ha yrkesaktivitet`(yrkesaktivitetId) {
                `skal ha antall dager`(31) // Hele januar
                `på dato`(LocalDate.of(2024, 1, 1)) {
                    `skal ha total grad`(100)
                    `skal ha refusjon`()
                }
                `på dato`(LocalDate.of(2024, 1, 2)) {
                    `skal ha total grad`(100)
                    `skal ha refusjon`()
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
                    `fra dato`(LocalDate.of(2024, 1, 1))
                    `til dato`(LocalDate.of(2024, 3, 31))
                }

                yrkesaktivitet {
                    id(yrkesaktivitetId)
                    `som arbeidstaker`()
                    `fra dato`(LocalDate.of(2024, 1, 10))
                    `er syk`(grad = 100, antallDager = 1)
                    `fra dato`(LocalDate.of(2024, 2, 10))
                    `er syk`(grad = 100, antallDager = 1)
                    `fra dato`(LocalDate.of(2024, 3, 10))
                    `er syk`(grad = 100, antallDager = 1)
                }

                inntekt {
                    yrkesaktivitetId(yrkesaktivitetId)
                    `med beløp`(50000) // 50 000 kr/mnd
                    `fra kilde`(Inntektskilde.AINNTEKT)
                    `med refusjon` {
                        `fra dato`(LocalDate.of(2024, 1, 1))
                        `til dato`(LocalDate.of(2024, 1, 15))
                        `med beløp`(10000) // 10 000 kr/mnd refusjon
                    }
                    `med refusjon` {
                        `fra dato`(LocalDate.of(2024, 2, 1))
                        `er åpen`()
                        `med beløp`(20000) // 20 000 kr/mnd refusjon
                    }
                }
            }

        resultat.skal {
            `ha yrkesaktivitet`(yrkesaktivitetId) {
                `skal ha antall dager`(91) // Jan-mars 2024
                `på dato`(LocalDate.of(2024, 1, 10)) {
                    `skal ha refusjon`() // Dag i lukket refusjonsperiode skal ha refusjon
                }
            }
        }
    }
}
