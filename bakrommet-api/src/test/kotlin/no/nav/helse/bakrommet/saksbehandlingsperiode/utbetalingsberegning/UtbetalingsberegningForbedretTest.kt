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

class UtbetalingsberegningForbedretTest {
    @Test
    fun `beregner utbetaling med åpen refusjonsperiode for arbeidstaker`() {
        val yrkesaktivitetId = UUID.randomUUID()

        val resultat =
            utbetalingsberegningTestOgBeregn {
                periode {
                    fra(1.januar(2024))
                    til(31.januar(2024))
                }

                yrkesaktivitet {
                    id(yrkesaktivitetId)
                    arbeidstaker("999333444")
                    fra(1.januar(2024))
                    syk(grad = 100, antallDager = 2)
                }

                inntekt {
                    yrkesaktivitetId(yrkesaktivitetId)
                    beløp(50000) // 50 000 kr/mnd
                    kilde(Inntektskilde.AINNTEKT)
                    refusjon {
                        fra(1.januar(2024))
                        åpen()
                        beløp(10000)
                    }
                }
            }

        resultat.skal {
            haYrkesaktivitet(yrkesaktivitetId) {
                harAntallDager(31) // Hele januar
                dag(1.januar(2024)) {
                    harGrad(100)
                    harRefusjon()
                }
                dag(2.januar(2024)) {
                    harGrad(100)
                    harRefusjon()
                }
            }

            haOppdrag {
                harAntallOppdrag(2) // Refusjon og person
                oppdrag(0) {
                    harFagområde("SPREF")
                    harNettoBeløp(922)
                    harMottaker("999333444")
                }
                oppdrag(1) {
                    harFagområde("SP")
                    harNettoBeløp(3694)
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

            haOppdrag {
                harAntallOppdrag(2)
                oppdrag(0) {
                    harNettoBeløp(461)
                    harFagområde("SPREF")
                }
                oppdrag(1) {
                    harNettoBeløp(1847)
                    harFagområde("SP")
                }
            }
        }
    }

    @Test
    fun `beregner utbetaling for inaktiv person`() {
        val yrkesaktivitetId = UUID.randomUUID()

        val resultat =
            utbetalingsberegningTestOgBeregn {
                periode {
                    fra(LocalDate.of(2024, 1, 1))
                    til(LocalDate.of(2024, 1, 31))
                }

                yrkesaktivitet {
                    id(yrkesaktivitetId)
                    inaktiv(variant = "INAKTIV_VARIANT_A")
                    this.fra(LocalDate.of(2024, 1, 1))
                    syk(grad = 100, antallDager = 5)
                }

                inntekt {
                    yrkesaktivitetId(yrkesaktivitetId)
                    beløp(30001) // 30 000 kr/mnd
                    kilde(Inntektskilde.AINNTEKT)
                }
            }

        resultat.skal {
            haYrkesaktivitet(yrkesaktivitetId) {
                harAntallDager(31) // Hele januar
                dag(LocalDate.of(2024, 1, 1)) {
                    harGrad(100)
                }
            }

            haOppdrag {
                harAntallOppdrag(1)
                oppdrag(0) {
                    harNettoBeløp(4500)
                    harFagområde("SP")
                }
            }
        }
    }

    @Test
    fun `beregner utbetaling for næringsdrivende`() {
        val yrkesaktivitetId = UUID.randomUUID()

        val input =
            utbetalingsberegningTest {
                periode {
                    fra(LocalDate.of(2024, 1, 1))
                    til(LocalDate.of(2024, 1, 31))
                }

                yrkesaktivitet {
                    id(yrkesaktivitetId)
                    næringsdrivende(forsikringstype = "FORSIKRING_80_PROSENT_FRA_FØRSTE_SYKEDAG")
                    this.fra(LocalDate.of(2024, 1, 1))
                    syk(grad = 100, antallDager = 5)
                }

                inntekt {
                    yrkesaktivitetId(yrkesaktivitetId)
                    beløp(40000) // 40 000 kr/mnd
                    kilde(Inntektskilde.AINNTEKT)
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

    @Test
    fun `beregner utbetaling med flere arbeidsforhold`() {
        val yrkesaktivitet1Id = UUID.randomUUID()
        val yrkesaktivitet2Id = UUID.randomUUID()

        val input =
            utbetalingsberegningTest {
                periode {
                    fra(LocalDate.of(2024, 1, 1))
                    til(LocalDate.of(2024, 1, 14))
                }

                yrkesaktivitet {
                    id(yrkesaktivitet1Id)
                    arbeidstaker()
                    this.fra(LocalDate.of(2024, 1, 1))
                    syk(grad = 100, antallDager = 14)
                }

                yrkesaktivitet {
                    id(yrkesaktivitet2Id)
                    arbeidstaker()
                    this.fra(LocalDate.of(2024, 1, 1))
                    syk(grad = 50, antallDager = 14)
                }

                inntekt {
                    yrkesaktivitetId(yrkesaktivitet1Id)
                    beløp(50000) // 50 000 kr/mnd
                    kilde(Inntektskilde.AINNTEKT)
                    refusjon {
                        fra(LocalDate.of(2024, 1, 1))
                        til(LocalDate.of(2024, 1, 7))
                        beløp(50000) // 50 000 kr/mnd refusjon
                    }
                    refusjon {
                        fra(LocalDate.of(2024, 1, 8))
                        åpen()
                        beløp(10000) // 10 000 kr/mnd refusjon
                    }
                }

                inntekt {
                    yrkesaktivitetId(yrkesaktivitet2Id)
                    beløp(50000) // 50 000 kr/mnd
                    kilde(Inntektskilde.AINNTEKT)
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
    fun `beregner utbetaling med blandet dagtyper`() {
        val yrkesaktivitetId = UUID.randomUUID()

        val input =
            utbetalingsberegningTest {
                periode {
                    fra(LocalDate.of(2024, 1, 1))
                    til(LocalDate.of(2024, 1, 29))
                }

                yrkesaktivitet {
                    id(yrkesaktivitetId)
                    arbeidstaker()
                    this.fra(LocalDate.of(2024, 1, 1))
                    arbeidsgiverperiode {
                        this.fra(LocalDate.of(2024, 1, 1))
                        til(LocalDate.of(2024, 1, 16))
                    }
                    syk(grad = 100, antallDager = 2)
                    arbeidsdag(antallDager = 1)
                    sykNav(grad = 100, antallDager = 2)
                    ferie(antallDager = 1)
                    permisjon(antallDager = 1)
                    avslått(begrunnelse = listOf("Ikke oppfylt krav"), antallDager = 1)
                    andreYtelser(begrunnelse = listOf("Dagpenger"), antallDager = 1)
                    syk(grad = 100, antallDager = 20)
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
    fun `beregner utbetaling med delvis sykefravær`() {
        val yrkesaktivitetId = UUID.randomUUID()

        val input =
            utbetalingsberegningTest {
                periode {
                    fra(LocalDate.of(2024, 1, 1))
                    til(LocalDate.of(2024, 1, 10))
                }

                yrkesaktivitet {
                    id(yrkesaktivitetId)
                    arbeidstaker()
                    this.fra(LocalDate.of(2024, 1, 1))
                    syk(grad = 50, antallDager = 5)
                    syk(grad = 25, antallDager = 3)
                    arbeidsdag(antallDager = 2)
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
            utbetalingsberegningTest {
                periode {
                    fra(LocalDate.of(2024, 1, 1))
                    til(LocalDate.of(2024, 1, 10))
                }

                yrkesaktivitet {
                    id(yrkesaktivitetId)
                    arbeidstaker()
                    this.fra(LocalDate.of(2024, 1, 1))
                    avslått(begrunnelse = listOf("Ikke oppfylt krav om medlemskap"), antallDager = 10)
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
    fun `debug test for å se JSON output`() {
        val yrkesaktivitet1Id = UUID.randomUUID()
        val yrkesaktivitet2Id = UUID.randomUUID()

        val input =
            utbetalingsberegningTest {
                periode {
                    fra(LocalDate.of(2024, 1, 1))
                    til(LocalDate.of(2024, 1, 14))
                }

                yrkesaktivitet {
                    id(yrkesaktivitet1Id)
                    arbeidstaker()
                    this.fra(LocalDate.of(2024, 1, 1))
                    syk(grad = 100, antallDager = 14)
                }

                yrkesaktivitet {
                    id(yrkesaktivitet2Id)
                    arbeidstaker()
                    this.fra(LocalDate.of(2024, 1, 1))
                    syk(grad = 50, antallDager = 14)
                }

                inntekt {
                    yrkesaktivitetId(yrkesaktivitet1Id)
                    beløp(50000) // 50 000 kr/mnd
                    kilde(Inntektskilde.AINNTEKT)
                    refusjon {
                        fra(LocalDate.of(2024, 1, 1))
                        til(LocalDate.of(2024, 1, 7))
                        beløp(50000) // 50 000 kr/mnd refusjon
                    }
                    refusjon {
                        fra(LocalDate.of(2024, 1, 8))
                        åpen()
                        beløp(10000) // 10 000 kr/mnd refusjon
                    }
                }

                inntekt {
                    yrkesaktivitetId(yrkesaktivitet2Id)
                    beløp(50000) // 50 000 kr/mnd
                    kilde(Inntektskilde.AINNTEKT)
                }
            }

        val resultat = beregnUtbetalingerForAlleYrkesaktiviteter(input)
        println(resultat.toJsonNode().toPrettyString())
    }
}
