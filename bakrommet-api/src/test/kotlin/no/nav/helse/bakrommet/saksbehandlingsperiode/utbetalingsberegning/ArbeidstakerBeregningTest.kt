package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.Inntektskilde
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.beregning.beregnUtbetalingerForAlleYrkesaktiviteter
import no.nav.helse.bakrommet.util.toJsonNode
import no.nav.helse.januar
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ArbeidstakerBeregningTest {
    @Test
    fun `beregner utbetaling for arbeidstaker med åpen refusjonsperiode`() {
        val yrkesaktivitetId = UUID.randomUUID()

        val resultat =
            utbetalingsberegningTestOgBeregn {
                periode {
                    `fra dato`(1.januar(2024))
                    `til dato`(31.januar(2024))
                }

                yrkesaktivitet {
                    id(yrkesaktivitetId)
                    `som arbeidstaker`(orgnummer = "999333444")
                    `fra dato`(1.januar(2024))
                    `er syk`(grad = 100, antallDager = 2)
                }

                inntekt {
                    yrkesaktivitetId(yrkesaktivitetId)
                    `med beløp`(50000)
                    `fra kilde`(Inntektskilde.AINNTEKT)
                    `med refusjon` {
                        `fra dato`(1.januar(2024))
                        `er åpen`()
                        `med beløp`(10000)
                    }
                }
            }

        resultat.skal {
            `ha yrkesaktivitet`(yrkesaktivitetId) {
                `skal ha antall dager`(31) // Hele januar
                `på dato`(1.januar(2024)) {
                    `skal ha total grad`(100)
                    `skal ha refusjon`()
                }
                `på dato`(2.januar(2024)) {
                    `skal ha total grad`(100)
                    `skal ha refusjon`()
                }
                `på dato`(3.januar(2024)) {
                    `skal ha total grad`(0)
                    `skal ha ingen refusjon`()
                }
            }

            `har oppdrag` {
                `skal ha antall oppdrag`(2) // Refusjon og person
                `oppdrag nummer`(0) {
                    `skal ha fagområde`("SPREF")
                    `skal ha netto beløp`(922)
                    `skal ha total beløp`(922)
                    `skal ha mottaker`("999333444")
                }
                `oppdrag nummer`(1) {
                    `skal ha fagområde`("SP")
                    `skal ha netto beløp`(3694)
                    `skal ha total beløp`(3694)
                }
            }
        }
    }

    @Test
    fun `beregner utbetaling for arbeidstaker med blandet refusjon (lukket og åpen)`() {
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
                    this.`fra dato`(LocalDate.of(2024, 1, 10))
                    `er syk`(grad = 100, antallDager = 1)
                    this.`fra dato`(LocalDate.of(2024, 2, 10))
                    `er syk`(grad = 100, antallDager = 1)
                    this.`fra dato`(LocalDate.of(2024, 3, 10))
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

            `har oppdrag` {
                `skal ha antall oppdrag`(2)
                `oppdrag nummer`(0) {
                    `skal ha netto beløp`(461)
                    `skal ha fagområde`("SPREF")
                }
                `oppdrag nummer`(1) {
                    `skal ha netto beløp`(1847)
                    `skal ha fagområde`("SP")
                }
            }
        }
    }

    @Test
    fun `beregner utbetaling for arbeidstaker med flere arbeidsforhold`() {
        val yrkesaktivitet1Id = UUID.randomUUID()
        val yrkesaktivitet2Id = UUID.randomUUID()

        val input =
            utbetalingsberegningTestdata {
                periode {
                    `fra dato`(LocalDate.of(2024, 1, 1))
                    `til dato`(LocalDate.of(2024, 1, 14))
                }

                yrkesaktivitet {
                    id(yrkesaktivitet1Id)
                    `som arbeidstaker`()
                    this.`fra dato`(LocalDate.of(2024, 1, 1))
                    `er syk`(grad = 100, antallDager = 14)
                }

                yrkesaktivitet {
                    id(yrkesaktivitet2Id)
                    `som arbeidstaker`()
                    this.`fra dato`(LocalDate.of(2024, 1, 1))
                    `er syk`(grad = 50, antallDager = 14)
                }

                inntekt {
                    yrkesaktivitetId(yrkesaktivitet1Id)
                    `med beløp`(50000) // 50 000 kr/mnd
                    `fra kilde`(Inntektskilde.AINNTEKT)
                    `med refusjon` {
                        `fra dato`(LocalDate.of(2024, 1, 1))
                        `til dato`(LocalDate.of(2024, 1, 7))
                        `med beløp`(50000) // 50 000 kr/mnd refusjon
                    }
                    `med refusjon` {
                        `fra dato`(LocalDate.of(2024, 1, 8))
                        `er åpen`()
                        `med beløp`(10000) // 10 000 kr/mnd refusjon
                    }
                }

                inntekt {
                    yrkesaktivitetId(yrkesaktivitet2Id)
                    `med beløp`(50000) // 50 000 kr/mnd
                    `fra kilde`(Inntektskilde.AINNTEKT)
                }
            }

        val resultat = beregnUtbetalingerForAlleYrkesaktiviteter(input)

        assertEquals(2, resultat.size)

        val yrkesaktivitet1Resultat = resultat.find { it.yrkesaktivitetId == yrkesaktivitet1Id }
        assertNotNull(yrkesaktivitet1Resultat)
        assertEquals(14, yrkesaktivitet1Resultat.utbetalingstidslinje.size)

        val yrkesaktivitet2Resultat = resultat.find { it.yrkesaktivitetId == yrkesaktivitet2Id }
        assertNotNull(yrkesaktivitet2Resultat)
        assertEquals(14, yrkesaktivitet2Resultat.utbetalingstidslinje.size)
    }

    @Test
    fun `beregner utbetaling for arbeidstaker med blandet dagtyper`() {
        val yrkesaktivitetId = UUID.randomUUID()

        val input =
            utbetalingsberegningTestdata {
                periode {
                    `fra dato`(LocalDate.of(2024, 1, 1))
                    `til dato`(LocalDate.of(2024, 1, 29))
                }

                yrkesaktivitet {
                    id(yrkesaktivitetId)
                    `som arbeidstaker`()
                    this.`fra dato`(LocalDate.of(2024, 1, 1))
                    `med arbeidsgiverperiode` {
                        this.`fra dato`(LocalDate.of(2024, 1, 1))
                        `til dato`(LocalDate.of(2024, 1, 16))
                    }
                    `er syk`(grad = 100, antallDager = 2)
                    `har arbeidsdager`(antallDager = 1)
                    `er syk nav`(grad = 100, antallDager = 2)
                    `har ferie`(antallDager = 1)
                    `har permisjon`(antallDager = 1)
                    `er avslått`(begrunnelse = listOf("Ikke oppfylt krav"), antallDager = 1)
                    `har andre ytelser`(begrunnelse = listOf("Dagpenger"), antallDager = 1)
                    `er syk`(grad = 100, antallDager = 20)
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

        val resultat = beregnUtbetalingerForAlleYrkesaktiviteter(input)

        assertEquals(1, resultat.size)
        val yrkesaktivitetResultat = resultat.first()
        assertEquals(29, yrkesaktivitetResultat.utbetalingstidslinje.size)

        // Sjekk at sykedagene har refusjon
        val sykedag = yrkesaktivitetResultat.utbetalingstidslinje.find { it.dato == LocalDate.of(2024, 1, 17) }
        assertNotNull(sykedag)
        assertTrue(
            sykedag.økonomi.arbeidsgiverbeløp != null && sykedag.økonomi.arbeidsgiverbeløp!!.dagligInt > 0,
            "Sykedag skal ha refusjon",
        )
    }

    @Test
    fun `beregner utbetaling for arbeidstaker med delvis sykefravær`() {
        val yrkesaktivitetId = UUID.randomUUID()

        val input =
            utbetalingsberegningTestdata {
                periode {
                    `fra dato`(LocalDate.of(2024, 1, 1))
                    `til dato`(LocalDate.of(2024, 1, 10))
                }

                yrkesaktivitet {
                    id(yrkesaktivitetId)
                    `som arbeidstaker`()
                    this.`fra dato`(LocalDate.of(2024, 1, 1))
                    `er syk`(grad = 50, antallDager = 5)
                    `er syk`(grad = 25, antallDager = 3)
                    `har arbeidsdager`(antallDager = 2)
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

        val resultat = beregnUtbetalingerForAlleYrkesaktiviteter(input)

        assertEquals(1, resultat.size)
        val yrkesaktivitetResultat = resultat.first()
        assertEquals(10, yrkesaktivitetResultat.utbetalingstidslinje.size)

        // Sjekk at delvis sykedager har riktig grad
        val delvisSykedag = yrkesaktivitetResultat.utbetalingstidslinje.find { it.dato == LocalDate.of(2024, 1, 1) }
        assertNotNull(delvisSykedag)
        assertEquals(50, delvisSykedag.økonomi.brukTotalGrad { it })
    }

    @Test
    fun `beregner utbetaling for arbeidstaker med alle dager avslått`() {
        val yrkesaktivitetId = UUID.randomUUID()

        val input =
            utbetalingsberegningTestdata {
                periode {
                    `fra dato`(LocalDate.of(2024, 1, 1))
                    `til dato`(LocalDate.of(2024, 1, 10))
                }

                yrkesaktivitet {
                    id(yrkesaktivitetId)
                    `som arbeidstaker`()
                    this.`fra dato`(LocalDate.of(2024, 1, 1))
                    `er avslått`(begrunnelse = listOf("Ikke oppfylt krav om medlemskap"), antallDager = 10)
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

        val resultat = beregnUtbetalingerForAlleYrkesaktiviteter(input)

        assertEquals(1, resultat.size)
        val yrkesaktivitetResultat = resultat.first()
        assertEquals(yrkesaktivitetId, yrkesaktivitetResultat.yrkesaktivitetId)
        assertEquals(10, yrkesaktivitetResultat.utbetalingstidslinje.size)

        // Sjekk at alle dager er avslått og har ingen utbetaling
        for (i in 1..10) {
            val avslåttDag =
                yrkesaktivitetResultat.utbetalingstidslinje.find {
                    it.dato == LocalDate.of(2024, 1, i)
                }
            assertNotNull(avslåttDag)
            assertEquals(0, avslåttDag.økonomi.brukTotalGrad { it })
            assertTrue(
                avslåttDag.økonomi.arbeidsgiverbeløp == null || avslåttDag.økonomi.arbeidsgiverbeløp!!.dagligInt == 0,
                "Avslått dag skal ikke ha arbeidsgiverbeløp",
            )
            assertTrue(
                avslåttDag.økonomi.personbeløp == null || avslåttDag.økonomi.personbeløp!!.dagligInt == 0,
                "Avslått dag skal ikke ha personbeløp",
            )
        }
    }

    @Test
    fun `debug test for å se JSON output med to arbeidsforhold`() {
        val yrkesaktivitet1Id = UUID.randomUUID()
        val yrkesaktivitet2Id = UUID.randomUUID()

        val input =
            utbetalingsberegningTestdata {
                periode {
                    `fra dato`(LocalDate.of(2024, 1, 1))
                    `til dato`(LocalDate.of(2024, 1, 14))
                }

                yrkesaktivitet {
                    id(yrkesaktivitet1Id)
                    `som arbeidstaker`()
                    this.`fra dato`(LocalDate.of(2024, 1, 1))
                    `er syk`(grad = 100, antallDager = 14)
                }

                yrkesaktivitet {
                    id(yrkesaktivitet2Id)
                    `som arbeidstaker`()
                    this.`fra dato`(LocalDate.of(2024, 1, 1))
                    `er syk`(grad = 50, antallDager = 14)
                }

                inntekt {
                    yrkesaktivitetId(yrkesaktivitet1Id)
                    `med beløp`(50000) // 50 000 kr/mnd
                    `fra kilde`(Inntektskilde.AINNTEKT)
                    `med refusjon` {
                        `fra dato`(LocalDate.of(2024, 1, 1))
                        `til dato`(LocalDate.of(2024, 1, 7))
                        `med beløp`(50000) // 50 000 kr/mnd refusjon
                    }
                    `med refusjon` {
                        `fra dato`(LocalDate.of(2024, 1, 8))
                        `er åpen`()
                        `med beløp`(10000) // 10 000 kr/mnd refusjon
                    }
                }

                inntekt {
                    yrkesaktivitetId(yrkesaktivitet2Id)
                    `med beløp`(50000) // 50 000 kr/mnd
                    `fra kilde`(Inntektskilde.AINNTEKT)
                }
            }

        val resultat = beregnUtbetalingerForAlleYrkesaktiviteter(input)
        println(resultat.toJsonNode().toPrettyString())
    }
}
