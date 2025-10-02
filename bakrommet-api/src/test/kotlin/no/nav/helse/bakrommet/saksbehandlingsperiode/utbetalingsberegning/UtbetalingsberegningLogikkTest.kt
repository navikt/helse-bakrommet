package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dag
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dagtype
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Kilde
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.Inntekt
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.Inntektskilde
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.Refusjonsperiode
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagResponse
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.Yrkesaktivitet
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.HashMap
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UtbetalingsberegningLogikkTest {
    @Test
    fun `beregner utbetaling med åpen refusjonsperiode`() {
        val yrkesaktivitetId = UUID.randomUUID()
        val saksbehandlingsperiode =
            Saksbehandlingsperiode(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 31),
            )

        val sykepengegrunnlag =
            lagSykepengegrunnlag(
                yrkesaktivitetId = yrkesaktivitetId,
                refusjon =
                    listOf(
                        Refusjonsperiode(
                            fom = LocalDate.of(2024, 1, 1),
                            tom = null,
                            // Åpen refusjonsperiode
                            beløpØre = 1000000L,
                            // 10 000 kr/mnd
                        ),
                    ),
            )

        val dagoversikt =
            listOf(
                Dag(
                    dato = LocalDate.of(2024, 1, 1),
                    dagtype = Dagtype.Syk,
                    grad = 100,
                    avslåttBegrunnelse = emptyList(),
                    kilde = Kilde.Saksbehandler,
                ),
                Dag(
                    dato = LocalDate.of(2024, 1, 2),
                    dagtype = Dagtype.Syk,
                    grad = 100,
                    avslåttBegrunnelse = emptyList(),
                    kilde = Kilde.Saksbehandler,
                ),
            )

        val yrkesaktivitet =
            lagYrkesaktivitet(
                id = yrkesaktivitetId,
                saksbehandlingsperiodeId = UUID.randomUUID(),
                dagoversikt = dagoversikt,
            )

        val input =
            UtbetalingsberegningInput(
                sykepengegrunnlag = sykepengegrunnlag,
                yrkesaktivitet = listOf(yrkesaktivitet),
                saksbehandlingsperiode = saksbehandlingsperiode,
            )

        val resultat = UtbetalingsberegningLogikk.beregnAlaSpleis(input)

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
        val saksbehandlingsperiode =
            Saksbehandlingsperiode(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 3, 31),
            )

        val sykepengegrunnlag =
            lagSykepengegrunnlag(
                yrkesaktivitetId = yrkesaktivitetId,
                refusjon =
                    listOf(
                        Refusjonsperiode(
                            fom = LocalDate.of(2024, 1, 1),
                            tom = LocalDate.of(2024, 1, 15),
                            // Lukket periode
                            beløpØre = 1000000L,
                            // 10 000 kr/mnd
                        ),
                        Refusjonsperiode(
                            fom = LocalDate.of(2024, 2, 1),
                            tom = null,
                            // Åpen periode
                            beløpØre = 2000000L,
                            // 20 000 kr/mnd
                        ),
                    ),
            )

        val yrkesaktivitet =
            lagYrkesaktivitet(
                id = yrkesaktivitetId,
                saksbehandlingsperiodeId = UUID.randomUUID(),
                dagoversikt =
                    listOf(
                        Dag(
                            dato = LocalDate.of(2024, 1, 10),
                            // I første refusjonsperiode
                            dagtype = Dagtype.Syk,
                            grad = 100,
                            avslåttBegrunnelse = emptyList(),
                            kilde = Kilde.Saksbehandler,
                        ),
                        Dag(
                            dato = LocalDate.of(2024, 2, 10),
                            // I andre refusjonsperiode
                            dagtype = Dagtype.Syk,
                            grad = 100,
                            avslåttBegrunnelse = emptyList(),
                            kilde = Kilde.Saksbehandler,
                        ),
                        Dag(
                            dato = LocalDate.of(2024, 3, 10),
                            // I åpen refusjonsperiode
                            dagtype = Dagtype.Syk,
                            grad = 100,
                            avslåttBegrunnelse = emptyList(),
                            kilde = Kilde.Saksbehandler,
                        ),
                    ),
            )

        val input =
            UtbetalingsberegningInput(
                sykepengegrunnlag = sykepengegrunnlag,
                yrkesaktivitet = listOf(yrkesaktivitet),
                saksbehandlingsperiode = saksbehandlingsperiode,
            )

        val resultat = UtbetalingsberegningLogikk.beregnAlaSpleis(input)

        // println(resultat)

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

    private fun lagSykepengegrunnlag(
        yrkesaktivitetId: UUID,
        refusjon: List<Refusjonsperiode>,
    ): SykepengegrunnlagResponse {
        return SykepengegrunnlagResponse(
            id = UUID.randomUUID(),
            saksbehandlingsperiodeId = UUID.randomUUID(),
            inntekter =
                listOf(
                    Inntekt(
                        yrkesaktivitetId = yrkesaktivitetId,
                        beløpPerMånedØre = 5000000L,
                        // 50 000 kr/mnd
                        kilde = Inntektskilde.AINNTEKT,
                        refusjon = refusjon,
                    ),
                ),
            totalInntektØre = 60000000L,
            // 50 000 * 12
            grunnbeløpØre = 12402800L,
            // 1G
            grunnbeløp6GØre = 74416800L,
            // 6G
            begrensetTil6G = false,
            sykepengegrunnlagØre = 60000000L,
            grunnbeløpVirkningstidspunkt = LocalDate.of(2024, 5, 1),
            opprettet = "2024-01-01T00:00:00Z",
            opprettetAv = "test",
            sistOppdatert = "2024-01-01T00:00:00Z",
        )
    }

    private fun lagYrkesaktivitet(
        id: UUID,
        saksbehandlingsperiodeId: UUID,
        dagoversikt: List<Dag>,
    ): Yrkesaktivitet {
        return Yrkesaktivitet(
            id = id,
            kategorisering =
                HashMap<String, String>().apply {
                    put("INNTEKTSKATEGORI", "ARBEIDSTAKER")
                },
            kategoriseringGenerert = null,
            dagoversikt = dagoversikt,
            dagoversiktGenerert = null,
            saksbehandlingsperiodeId = saksbehandlingsperiodeId,
            opprettet = OffsetDateTime.now(),
            generertFraDokumenter = emptyList(),
        )
    }
}
