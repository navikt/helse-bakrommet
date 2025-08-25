package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dag
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dagtype
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagResponse
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.Yrkesaktivitet
import no.nav.helse.bakrommet.util.objectMapper
import java.time.LocalDate
import java.util.UUID

object UtbetalingsberegningLogikk {
    /**
     * Beregner sykepengegrunnlaget basert på input data
     *
     * @param input Input data for beregningen
     * @return BeregningData med resultatet
     */
    fun beregn(input: UtbetalingsberegningInput): UtbetalingsberegningData {
        // Beregn dagsats (sykepengegrunnlag delt på 260)
        val dagsatsØre = input.sykepengegrunnlag.sykepengegrunnlagØre / 260L

        // Opprett beregning for hver inntektsforhold
        val yrkesaktiviteter =
            input.yrkesaktivitet.map { inntektsforhold ->
                val dagoversikt = hentDagoversiktFraInntektsforhold(inntektsforhold)
                val dagBeregninger =
                    dagoversikt.map { dag ->
                        beregnDag(dag, dagsatsØre, input.sykepengegrunnlag, inntektsforhold.id)
                    }

                YrkesaktivitetUtbetalingsberegning(
                    yrkesaktivitetId = inntektsforhold.id,
                    dager = dagBeregninger,
                )
            }

        return UtbetalingsberegningData(yrkesaktiviteter = yrkesaktiviteter)
    }

    private fun hentDagoversiktFraInntektsforhold(yrkesaktivitet: Yrkesaktivitet): List<Dag> {
        val dagoversiktJson = yrkesaktivitet.dagoversikt ?: return emptyList()

        return try {
            if (dagoversiktJson.isArray) {
                dagoversiktJson.map { dagJson ->
                    objectMapper.treeToValue(dagJson, Dag::class.java)
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun beregnDag(
        dag: Dag,
        dagsatsØre: Long,
        sykepengegrunnlag: SykepengegrunnlagResponse,
        inntektsforholdId: UUID,
    ): DagUtbetalingsberegning {
        // Beregn utbetaling basert på grad og dagtype
        val utbetalingØre =
            when (dag.dagtype) {
                Dagtype.Syk, Dagtype.SykNav -> {
                    val grad = dag.grad ?: 100
                    (dagsatsØre * grad) / 100L
                }
                else -> 0L
            }

        // Finn refusjon for denne dagen fra sykepengegrunnlaget
        val refusjonØre = finnRefusjonForDag(dag.dato, sykepengegrunnlag, inntektsforholdId)

        return DagUtbetalingsberegning(
            dato = dag.dato,
            utbetalingØre = utbetalingØre,
            refusjonØre = refusjonØre,
        )
    }

    private fun finnRefusjonForDag(
        dato: LocalDate,
        sykepengegrunnlag: SykepengegrunnlagResponse,
        inntektsforholdId: UUID,
    ): Long {
        // Finn refusjon fra sykepengegrunnlaget for denne datoen og inntektsforholdet
        return sykepengegrunnlag.inntekter
            .filter { it.inntektsforholdId == inntektsforholdId }
            .flatMap { inntekt ->
                inntekt.refusjon.filter { refusjon ->
                    dato in refusjon.fom..refusjon.tom
                }
            }
            .sumOf { it.beløpØre }
    }
}
