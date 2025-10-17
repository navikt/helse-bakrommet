package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import no.nav.helse.bakrommet.testutils.`should equal`
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
                skjæringstidspunkt(LocalDate.of(2024, 1, 1))

                yrkesaktivitet {
                    id(yrkesaktivitetId)
                    `som inaktiv`(variant = "INAKTIV_VARIANT_A")
                    `fra dato`(LocalDate.of(2024, 1, 1))
                    `er syk`(grad = 100, antallDager = 5)
                    `med inntektData` {
                        `med beløp`(30000) // 30 000 kr/mnd
                    }
                }
            }

        resultat.sykepengegrunnlag.sykepengegrunnlag.beløp `should equal` 360000.0
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
