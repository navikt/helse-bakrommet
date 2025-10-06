package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.Inntektskilde
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.beregning.beregnUtbetalingerForAlleYrkesaktiviteter
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UtbetalingsberegningLogikkTest {
    @Test
    fun `beregner utbetaling med åpen refusjonsperiode`() {
        val yrkesaktivitetId = UUID.randomUUID()

        val input =
            utbetalingsberegningTest {
                periode(fom = LocalDate.of(2024, 1, 1), tom = LocalDate.of(2024, 1, 31))

                yrkesaktivitet {
                    id(yrkesaktivitetId)
                    arbeidstaker()
                    fra(LocalDate.of(2024, 1, 1))
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

        val resultat = beregnUtbetalingerForAlleYrkesaktiviteter(input)

        assertEquals(1, resultat.size)
        val yrkesaktivitetResultat = resultat.first()
        assertEquals(yrkesaktivitetId, yrkesaktivitetResultat.yrkesaktivitetId)

        // Vi skal ha 31 dager (hele januar)
        assertEquals(31, yrkesaktivitetResultat.utbetalingstidslinje.size)

        // Sjekk at sykedagene har refusjon
        val sykedag1 = yrkesaktivitetResultat.utbetalingstidslinje.find { it.dato == LocalDate.of(2024, 1, 1) }
        assertNotNull(sykedag1)
        assertEquals(100, sykedag1.økonomi.brukTotalGrad { it })
        assertTrue(
            sykedag1.økonomi.arbeidsgiverbeløp != null && sykedag1.økonomi.arbeidsgiverbeløp!!.dagligInt > 0,
            "Sykedag skal ha refusjon",
        )

        val sykedag2 = yrkesaktivitetResultat.utbetalingstidslinje.find { it.dato == LocalDate.of(2024, 1, 2) }
        assertNotNull(sykedag2)
        assertEquals(100, sykedag2.økonomi.brukTotalGrad { it })
        assertTrue(
            sykedag2.økonomi.arbeidsgiverbeløp != null && sykedag2.økonomi.arbeidsgiverbeløp!!.dagligInt > 0,
            "Sykedag skal ha refusjon",
        )
    }

    @Test
    fun `beregner utbetaling med blandet refusjon (lukket og åpen)`() {
        val yrkesaktivitetId = UUID.randomUUID()

        val input =
            utbetalingsberegningTest {
                periode(fom = LocalDate.of(2024, 1, 1), tom = LocalDate.of(2024, 3, 31))

                yrkesaktivitet {
                    id(yrkesaktivitetId)
                    arbeidstaker()
                    fra(LocalDate.of(2024, 1, 10))
                    syk(grad = 100, antallDager = 1)
                    fra(LocalDate.of(2024, 2, 10))
                    syk(grad = 100, antallDager = 1)
                    fra(LocalDate.of(2024, 3, 10))
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

        val resultat = beregnUtbetalingerForAlleYrkesaktiviteter(input)

        assertEquals(1, resultat.size)
        val yrkesaktivitetResultat = resultat.first()

        // Vi skal ha 91 dager (jan-mars 2024)
        assertEquals(91, yrkesaktivitetResultat.utbetalingstidslinje.size)

        // Sjekk at alle sykedagene har refusjon
        val sykedag1 = yrkesaktivitetResultat.utbetalingstidslinje.find { it.dato == LocalDate.of(2024, 1, 10) }
        assertNotNull(sykedag1)
        assertTrue(
            sykedag1.økonomi.arbeidsgiverbeløp != null && sykedag1.økonomi.arbeidsgiverbeløp!!.dagligInt > 0,
            "Dag i lukket refusjonsperiode skal ha refusjon",
        )
    }
}
