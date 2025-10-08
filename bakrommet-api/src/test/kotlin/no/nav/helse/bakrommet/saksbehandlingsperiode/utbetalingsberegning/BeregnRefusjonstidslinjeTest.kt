package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.beregning.beregnRefusjonstidslinje
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.mars
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class BeregnRefusjonstidslinjeTest {
    @Test
    fun `beregner refusjonstidslinje med lukket refusjonsperiode`() {
        val yrkesaktivitetId = UUID.randomUUID()

        val input =
            utbetalingsberegningTestdata {
                periode {
                    `fra dato`(1.januar(2024))
                    `til dato`(31.mars(2024))
                }

                yrkesaktivitet {
                    id(yrkesaktivitetId)
                    `som arbeidstaker`("999333444")
                }

                inntekt {
                    yrkesaktivitetId(yrkesaktivitetId)
                    `med beløp`(50000) // 50 000 kr/mnd
                    `med refusjon` {
                        `fra dato`(1.januar(2024))
                        `til dato`(31.januar(2024))
                        `med beløp`(10000) // 10 000 kr/mnd
                    }
                }
            }

        val refusjonstidslinje =
            beregnRefusjonstidslinje(
                input.sykepengegrunnlag,
                yrkesaktivitetId,
                input.saksbehandlingsperiode,
            )

        // Januar har 31 dager, så vi skal ha 31 dager med refusjon
        assertEquals(31, refusjonstidslinje.size)

        // Sjekk at alle dager i refusjonsperioden har refusjon
        var aktuellDato = 1.januar(2024)
        while (!aktuellDato.isAfter(31.januar(2024))) {
            assertNotNull(refusjonstidslinje[aktuellDato], "Dato $aktuellDato skal ha refusjon")
            aktuellDato = aktuellDato.plusDays(1)
        }

        // Sjekk at dager utenfor refusjonsperioden ikke har refusjon
        assertEquals(null, refusjonstidslinje[1.februar(2024)])
    }

    @Test
    fun `beregner refusjonstidslinje med åpen refusjonsperiode`() {
        val yrkesaktivitetId = UUID.randomUUID()

        val input =
            utbetalingsberegningTestdata {
                periode {
                    `fra dato`(1.januar(2024))
                    `til dato`(31.mars(2024))
                }

                yrkesaktivitet {
                    id(yrkesaktivitetId)
                    `som arbeidstaker`("999333444")
                }

                inntekt {
                    yrkesaktivitetId(yrkesaktivitetId)
                    `med beløp`(50000) // 50 000 kr/mnd
                    `med refusjon` {
                        `fra dato`(1.januar(2024))
                        `er åpen`() // Åpen refusjonsperiode
                        `med beløp`(10000) // 10 000 kr/mnd
                    }
                }
            }

        val refusjonstidslinje =
            beregnRefusjonstidslinje(
                input.sykepengegrunnlag,
                yrkesaktivitetId,
                input.saksbehandlingsperiode,
            )

        // Hele saksbehandlingsperioden skal ha refusjon (jan-mars = 91 dager)
        // Januar: 31, Februar: 29 (2024 er skuddår), Mars: 31 = 91 dager
        assertEquals(91, refusjonstidslinje.size)

        // Sjekk at alle dager i saksbehandlingsperioden har refusjon
        var aktuellDato = input.saksbehandlingsperiode.fom
        while (!aktuellDato.isAfter(input.saksbehandlingsperiode.tom)) {
            assertNotNull(refusjonstidslinje[aktuellDato], "Dato $aktuellDato skal ha refusjon")
            aktuellDato = aktuellDato.plusDays(1)
        }
    }

    @Test
    fun `beregner refusjonstidslinje med flere refusjonsperioder`() {
        val yrkesaktivitetId = UUID.randomUUID()

        val input =
            utbetalingsberegningTestdata {
                periode {
                    `fra dato`(1.januar(2024))
                    `til dato`(31.mars(2024))
                }

                yrkesaktivitet {
                    id(yrkesaktivitetId)
                    `som arbeidstaker`("999333444")
                }

                inntekt {
                    yrkesaktivitetId(yrkesaktivitetId)
                    `med beløp`(50000) // 50 000 kr/mnd
                    `med refusjon` {
                        `fra dato`(1.januar(2024))
                        `til dato`(15.januar(2024))
                        `med beløp`(10000) // 10 000 kr/mnd
                    }
                    `med refusjon` {
                        `fra dato`(1.februar(2024))
                        `er åpen`() // Åpen periode
                        `med beløp`(20000) // 20 000 kr/mnd
                    }
                }
            }

        val refusjonstidslinje =
            beregnRefusjonstidslinje(
                input.sykepengegrunnlag,
                yrkesaktivitetId,
                input.saksbehandlingsperiode,
            )

        // Første periode: 15 dager (1-15 jan)
        // Andre periode: 60 dager (1 feb - 31 mar, siden det er åpen periode)
        // Total: 75 dager
        assertEquals(75, refusjonstidslinje.size)

        // Sjekk at første periode har refusjon
        var aktuellDato = 1.januar(2024)
        while (!aktuellDato.isAfter(15.januar(2024))) {
            assertNotNull(refusjonstidslinje[aktuellDato], "Dato $aktuellDato skal ha refusjon fra første periode")
            aktuellDato = aktuellDato.plusDays(1)
        }

        // Sjekk at andre periode har refusjon
        aktuellDato = 1.februar(2024)
        while (!aktuellDato.isAfter(31.mars(2024))) {
            assertNotNull(refusjonstidslinje[aktuellDato], "Dato $aktuellDato skal ha refusjon fra andre periode")
            aktuellDato = aktuellDato.plusDays(1)
        }

        // Sjekk at dager utenfor refusjonsperioder ikke har refusjon
        assertEquals(null, refusjonstidslinje[16.januar(2024)])
        assertEquals(null, refusjonstidslinje[31.januar(2024)])
    }
}
