package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.Inntekt
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.Inntektskilde
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.Refusjonsperiode
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagResponse
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RefusjonstidslinjeUtilityTest {
    @Test
    fun `beregner refusjonstidslinje med lukket refusjonsperiode`() {
        val yrkesaktivitetId = UUID.randomUUID()
        val fom = LocalDate.of(2024, 1, 1)
        val tom = LocalDate.of(2024, 1, 31)

        val sykepengegrunnlag =
            lagSykepengegrunnlag(
                yrkesaktivitetId = yrkesaktivitetId,
                refusjon =
                    listOf(
                        Refusjonsperiode(
                            fom = fom,
                            tom = tom,
                            beløpØre = 1000000L,
                            // 10 000 kr/mnd
                        ),
                    ),
            )

        val saksbehandlingsperiode =
            Saksbehandlingsperiode(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 3, 31),
            )

        val refusjonstidslinje =
            RefusjonstidslinjeUtility.beregnRefusjonstidslinje(
                sykepengegrunnlag,
                yrkesaktivitetId,
                saksbehandlingsperiode,
            )

        // Januar har 31 dager, så vi skal ha 31 dager med refusjon
        assertEquals(31, refusjonstidslinje.size)

        // Sjekk at alle dager i refusjonsperioden har refusjon
        var aktuellDato = fom
        while (!aktuellDato.isAfter(tom)) {
            assertNotNull(refusjonstidslinje[aktuellDato], "Dato $aktuellDato skal ha refusjon")
            aktuellDato = aktuellDato.plusDays(1)
        }

        // Sjekk at dager utenfor refusjonsperioden ikke har refusjon
        assertEquals(null, refusjonstidslinje[LocalDate.of(2024, 2, 1)])
    }

    @Test
    fun `beregner refusjonstidslinje med åpen refusjonsperiode`() {
        val yrkesaktivitetId = UUID.randomUUID()
        val fom = LocalDate.of(2024, 1, 1)
        // tom = null (åpen periode)

        val sykepengegrunnlag =
            lagSykepengegrunnlag(
                yrkesaktivitetId = yrkesaktivitetId,
                refusjon =
                    listOf(
                        Refusjonsperiode(
                            fom = fom,
                            tom = null,
                            // Åpen refusjonsperiode
                            beløpØre = 1000000L,
                            // 10 000 kr/mnd
                        ),
                    ),
            )

        val saksbehandlingsperiode =
            Saksbehandlingsperiode(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 3, 31),
            )

        val refusjonstidslinje =
            RefusjonstidslinjeUtility.beregnRefusjonstidslinje(
                sykepengegrunnlag,
                yrkesaktivitetId,
                saksbehandlingsperiode,
            )

        // Hele saksbehandlingsperioden skal ha refusjon (jan-mars = 91 dager)
        // Januar: 31, Februar: 29 (2024 er skuddår), Mars: 31 = 91 dager
        assertEquals(91, refusjonstidslinje.size)

        // Sjekk at alle dager i saksbehandlingsperioden har refusjon
        var aktuellDato = saksbehandlingsperiode.fom
        while (!aktuellDato.isAfter(saksbehandlingsperiode.tom)) {
            assertNotNull(refusjonstidslinje[aktuellDato], "Dato $aktuellDato skal ha refusjon")
            aktuellDato = aktuellDato.plusDays(1)
        }
    }

    @Test
    fun `beregner refusjonstidslinje med flere refusjonsperioder`() {
        val yrkesaktivitetId = UUID.randomUUID()

        val sykepengegrunnlag =
            lagSykepengegrunnlag(
                yrkesaktivitetId = yrkesaktivitetId,
                refusjon =
                    listOf(
                        Refusjonsperiode(
                            fom = LocalDate.of(2024, 1, 1),
                            tom = LocalDate.of(2024, 1, 15),
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

        val saksbehandlingsperiode =
            Saksbehandlingsperiode(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 3, 31),
            )

        val refusjonstidslinje =
            RefusjonstidslinjeUtility.beregnRefusjonstidslinje(
                sykepengegrunnlag,
                yrkesaktivitetId,
                saksbehandlingsperiode,
            )

        // Første periode: 15 dager (1-15 jan)
        // Andre periode: 60 dager (1 feb - 31 mar, siden det er åpen periode)
        // Total: 75 dager
        assertEquals(75, refusjonstidslinje.size)

        // Sjekk at første periode har refusjon
        var aktuellDato = LocalDate.of(2024, 1, 1)
        while (!aktuellDato.isAfter(LocalDate.of(2024, 1, 15))) {
            assertNotNull(refusjonstidslinje[aktuellDato], "Dato $aktuellDato skal ha refusjon fra første periode")
            aktuellDato = aktuellDato.plusDays(1)
        }

        // Sjekk at andre periode har refusjon
        aktuellDato = LocalDate.of(2024, 2, 1)
        while (!aktuellDato.isAfter(LocalDate.of(2024, 3, 31))) {
            assertNotNull(refusjonstidslinje[aktuellDato], "Dato $aktuellDato skal ha refusjon fra andre periode")
            aktuellDato = aktuellDato.plusDays(1)
        }

        // Sjekk at dager utenfor refusjonsperioder ikke har refusjon
        assertEquals(null, refusjonstidslinje[LocalDate.of(2024, 1, 16)])
        assertEquals(null, refusjonstidslinje[LocalDate.of(2024, 1, 31)])
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
}
