package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.Inntektskilde
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class InaktivBeregningTest {
    @Test
    fun `beregner utbetaling for inaktiv person`() {
        val yrkesaktivitetId = UUID.randomUUID()

        val resultat =
            utbetalingsberegningTestOgBeregn {
                periode {
                    `fra dato`(LocalDate.of(2024, 1, 1))
                    `til dato`(LocalDate.of(2024, 1, 31))
                }

                yrkesaktivitet {
                    id(yrkesaktivitetId)
                    `som inaktiv`(variant = "INAKTIV_VARIANT_A")
                    this.`fra dato`(LocalDate.of(2024, 1, 1))
                    `er syk`(grad = 100, antallDager = 5)
                }

                inntekt {
                    yrkesaktivitetId(yrkesaktivitetId)
                    `med beløp`(30001) // 30 000 kr/mnd
                    `fra kilde`(Inntektskilde.AINNTEKT)
                }
            }

        resultat.skal {
            `ha yrkesaktivitet`(yrkesaktivitetId) {
                `skal ha antall dager`(31) // Hele januar
                `på dato`(LocalDate.of(2024, 1, 1)) {
                    `skal ha total grad`(100)
                }
            }

            `har oppdrag` {
                `skal ha antall oppdrag`(1)
                `oppdrag nummer`(0) {
                    `skal ha netto beløp`(4500)
                    `skal ha fagområde`("SP")
                }
            }
        }
    }
}
