package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.Inntekt
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.Inntektskilde
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.Refusjonsperiode
import no.nav.helse.bakrommet.util.toJsonNode
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class UtbetalingViaSpleisKodeTest {
    fun Int.krSomØre() = this * 100L

    @Test
    fun `to yrkesaktiviteter`() {
        val førsteDag = LocalDate.of(2024, 1, 1)

        val saksbehandlingsperiode =
            Saksbehandlingsperiode(
                fom = førsteDag,
                tom = førsteDag.plusDays(13),
            )
        val saksbehandlingsperiodeId = UUID.randomUUID()

        val yrkesaktivitet1 =
            lagYrkesaktivitet(
                saksbehandlingsperiodeId = saksbehandlingsperiodeId,
                dagoversikt =
                    DagListeBuilder(førsteDag).apply {
                        repeat(14) {
                            syk(grad = 100)
                        }
                    }.dager,
            )

        val yrkesaktivitet2 =
            lagYrkesaktivitet(
                saksbehandlingsperiodeId = saksbehandlingsperiodeId,
                dagoversikt =
                    DagListeBuilder(førsteDag).apply {
                        repeat(14) {
                            syk(grad = 50)
                        }
                    }.dager,
            )

        val sykepengegrunnlag =
            sykepengegrunnlag(
                inntekter =
                    listOf(
                        Inntekt(
                            yrkesaktivitetId = yrkesaktivitet1.id,
                            beløpPerMånedØre = 50_000.krSomØre(),
                            kilde = Inntektskilde.AINNTEKT,
                            refusjon =
                                listOf(
                                    Refusjonsperiode(
                                        fom = førsteDag,
                                        tom = førsteDag.plusDays(7),
                                        // Lukket periode
                                        beløpØre = 50_000.krSomØre(),
                                    ),
                                    Refusjonsperiode(
                                        fom = førsteDag.plusDays(8),
                                        tom = null,
                                        // Åpen periode
                                        beløpØre = 10_000.krSomØre(),
                                    ),
                                ),
                        ),
                        Inntekt(
                            yrkesaktivitetId = yrkesaktivitet2.id,
                            beløpPerMånedØre = 50_000.krSomØre(),
                            kilde = Inntektskilde.AINNTEKT,
                            refusjon = emptyList(),
                        ),
                    ),
            )
        val input =
            UtbetalingsberegningInput(
                sykepengegrunnlag = sykepengegrunnlag,
                yrkesaktivitet = listOf(yrkesaktivitet1, yrkesaktivitet2),
                saksbehandlingsperiode = saksbehandlingsperiode,
            )

        val res2 = UtbetalingsberegningLogikk.beregnAlaSpleis(input)

        println(res2.toJsonNode().toPrettyString())
    }
}
