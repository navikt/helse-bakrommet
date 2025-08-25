package no.nav.helse.bakrommet.saksbehandlingsperiode.beregning

import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dag
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dagtype
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntektsforhold.Inntektsforhold
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagResponse
import no.nav.helse.bakrommet.util.objectMapper
import java.time.LocalDate
import java.util.UUID

object BeregningLogikk {
    /**
     * Beregner sykepengegrunnlaget basert på input data
     *
     * @param input Input data for beregningen
     * @return BeregningData med resultatet
     */
    fun beregn(input: BeregningInput): BeregningData {
        // Beregn dagsats (sykepengegrunnlag delt på 260)
        val dagsatsØre = input.sykepengegrunnlag.sykepengegrunnlagØre / 260L

        // Opprett beregning for hver inntektsforhold
        val yrkesaktiviteter =
            input.inntektsforhold.map { inntektsforhold ->
                val dagoversikt = hentDagoversiktFraInntektsforhold(inntektsforhold)
                val dagBeregninger =
                    dagoversikt.map { dag ->
                        beregnDag(dag, dagsatsØre, input.sykepengegrunnlag, inntektsforhold.id)
                    }

                YrkesaktivitetBeregning(
                    yrkesaktivitetId = inntektsforhold.id,
                    dager = dagBeregninger,
                )
            }

        return BeregningData(yrkesaktiviteter = yrkesaktiviteter)
    }

    private fun hentDagoversiktFraInntektsforhold(inntektsforhold: Inntektsforhold): List<Dag> {
        val dagoversiktJson = inntektsforhold.dagoversikt ?: return emptyList()

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
    ): DagBeregning {
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

        return DagBeregning(
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
