package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagResponse
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.Yrkesaktivitet
import no.nav.helse.bakrommet.util.asJsonNode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.Inntekt as SykepengegrunnlagInntekt

class UtbetalingsberegningLogikkTest {
    @Test
    fun `skal beregne utbetaling for enkelt sykedag`() {
        // Given
        val yrkesaktivitetId = UUID.randomUUID()
        val input =
            lagTestInput(
                yrkesaktivitet = listOf(lagSykYrkesaktivitet(yrkesaktivitetId)),
                // 50 000 kr
                sykepengegrunnlag = lagSykepengegrunnlag(yrkesaktivitetId, 5000000),
            )

        // When
        val resultat = UtbetalingsberegningLogikk.beregn(input)

        // Then
        assertEquals(1, resultat.yrkesaktiviteter.size)
        assertEquals(yrkesaktivitetId, resultat.yrkesaktiviteter[0].yrkesaktivitetId)
        assertEquals(1, resultat.yrkesaktiviteter[0].dager.size)
        assertTrue(resultat.yrkesaktiviteter[0].dager[0].utbetalingØre > 0)
    }

    @Test
    fun `skal håndtere manglende inntekt for yrkesaktivitet`() {
        // Given
        val yrkesaktivitetId = UUID.randomUUID()
        val input =
            lagTestInput(
                yrkesaktivitet = listOf(lagSykYrkesaktivitet(yrkesaktivitetId)),
                // Feil yrkesaktivitetId
                sykepengegrunnlag = lagSykepengegrunnlag(UUID.randomUUID(), 5000000),
            )

        // When/Then
        val exception =
            assertThrows(UtbetalingsberegningFeil.ManglendeInntekt::class.java) {
                UtbetalingsberegningLogikk.beregn(input)
            }
        assertEquals(yrkesaktivitetId, exception.yrkesaktivitetId)
    }

    @Test
    fun `skal fylle ut manglende dager som arbeidsdager`() {
        // Given
        val yrkesaktivitetId = UUID.randomUUID()
        val input =
            lagTestInput(
                yrkesaktivitet = listOf(lagSykYrkesaktivitet(yrkesaktivitetId)),
                saksbehandlingsperiode =
                    Saksbehandlingsperiode(
                        fom = LocalDate.of(2024, 1, 1),
                        tom = LocalDate.of(2024, 1, 3),
                    ),
            )

        // When
        val resultat = UtbetalingsberegningLogikk.beregn(input)

        // Then
        assertEquals(3, resultat.yrkesaktiviteter[0].dager.size)
        assertEquals(LocalDate.of(2024, 1, 1), resultat.yrkesaktiviteter[0].dager[0].dato)
        assertEquals(LocalDate.of(2024, 1, 2), resultat.yrkesaktiviteter[0].dager[1].dato)
        assertEquals(LocalDate.of(2024, 1, 3), resultat.yrkesaktiviteter[0].dager[2].dato)
    }

    @Test
    fun `skal håndtere ugyldig periode`() {
        // Given
        val yrkesaktivitetId = UUID.randomUUID()
        val input =
            lagTestInput(
                yrkesaktivitet = listOf(lagSykYrkesaktivitet(yrkesaktivitetId)),
                saksbehandlingsperiode =
                    Saksbehandlingsperiode(
                        // Tom før fom
                        fom = LocalDate.of(2024, 1, 3),
                        tom = LocalDate.of(2024, 1, 1),
                    ),
            )

        // When/Then
        assertThrows(UtbetalingsberegningFeil.UgyldigPeriode::class.java) {
            UtbetalingsberegningLogikk.beregn(input)
        }
    }

    private fun lagTestInput(
        yrkesaktivitet: List<Yrkesaktivitet>,
        sykepengegrunnlag: SykepengegrunnlagResponse = lagSykepengegrunnlag(yrkesaktivitet.first().id, 5000000),
        saksbehandlingsperiode: Saksbehandlingsperiode =
            Saksbehandlingsperiode(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 1),
            ),
    ): UtbetalingsberegningInput {
        return UtbetalingsberegningInput(
            yrkesaktivitet = yrkesaktivitet,
            sykepengegrunnlag = sykepengegrunnlag,
            saksbehandlingsperiode = saksbehandlingsperiode,
        )
    }

    private fun lagSykYrkesaktivitet(yrkesaktivitetId: UUID): Yrkesaktivitet {
        return Yrkesaktivitet(
            id = yrkesaktivitetId,
            kategorisering = "{}".asJsonNode(),
            kategoriseringGenerert = null,
            dagoversikt = """[{"dato":"2024-01-01","dagtype":"Syk","grad":100,"avvistBegrunnelse":[],"kilde":null}]""".asJsonNode(),
            dagoversiktGenerert = null,
            saksbehandlingsperiodeId = UUID.randomUUID(),
            opprettet = java.time.OffsetDateTime.now(),
            generertFraDokumenter = emptyList(),
        )
    }

    private fun lagSykepengegrunnlag(
        yrkesaktivitetId: UUID,
        beløpPerMånedØre: Long,
    ): SykepengegrunnlagResponse {
        return SykepengegrunnlagResponse(
            id = UUID.randomUUID(),
            saksbehandlingsperiodeId = UUID.randomUUID(),
            inntekter =
                listOf(
                    SykepengegrunnlagInntekt(
                        yrkesaktivitetId = yrkesaktivitetId,
                        beløpPerMånedØre = beløpPerMånedØre,
                        kilde = no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.Inntektskilde.AINNTEKT,
                        refusjon = emptyList(),
                    ),
                ),
            totalInntektØre = beløpPerMånedØre * 12,
            // 1000 kr
            grunnbeløpØre = 100000,
            // 600 000 kr (6G)
            grunnbeløp6GØre = 60000000,
            begrensetTil6G = false,
            // 600 000 kr (6G)
            sykepengegrunnlagØre = 60000000,
            grunnbeløpVirkningstidspunkt = LocalDate.of(2024, 1, 1),
            opprettet = "2024-01-01T00:00:00",
            opprettetAv = "test",
            sistOppdatert = "2024-01-01T00:00:00",
        )
    }
}
