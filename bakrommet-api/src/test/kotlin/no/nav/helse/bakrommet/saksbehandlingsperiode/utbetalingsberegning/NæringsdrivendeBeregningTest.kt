package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.beregning.beregnUtbetalingerForAlleYrkesaktiviteter
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class NæringsdrivendeBeregningTest {
    @Test
    fun `beregner utbetaling for næringsdrivende`() {
        val yrkesaktivitetId = UUID.randomUUID()

        val input =
            utbetalingsberegningTestdata {
                periode {
                    `fra dato`(LocalDate.of(2024, 1, 1))
                    `til dato`(LocalDate.of(2024, 1, 31))
                }
                skjæringstidspunkt(LocalDate.of(2024, 1, 1))

                yrkesaktivitet {
                    id(yrkesaktivitetId)
                    `som næringsdrivende`(forsikringstype = "FORSIKRING_80_PROSENT_FRA_FØRSTE_SYKEDAG")
                    this.`fra dato`(LocalDate.of(2024, 1, 1))
                    `er syk`(grad = 100, antallDager = 5)
                    `med inntektData` {
                        `med beløp`(40000) // 40 000 kr/mnd
                    }
                }
            }

        val resultat = beregnUtbetalingerForAlleYrkesaktiviteter(input)
        val oppdrage = resultat
        assertEquals(1, resultat.size)
        val yrkesaktivitetResultat = resultat.first()
        assertEquals(yrkesaktivitetId, yrkesaktivitetResultat.yrkesaktivitetId)
        assertEquals(31, yrkesaktivitetResultat.utbetalingstidslinje.size)

        // Sjekk at sykedagene er beregnet for næringsdrivende
        val sykedag = yrkesaktivitetResultat.utbetalingstidslinje.find { it.dato == LocalDate.of(2024, 1, 1) }
        assertNotNull(sykedag)
        assertEquals(100, sykedag.økonomi.brukTotalGrad { it })
    }
}
