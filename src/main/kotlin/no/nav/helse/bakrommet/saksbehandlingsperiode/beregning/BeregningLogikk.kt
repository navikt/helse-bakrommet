package no.nav.helse.bakrommet.saksbehandlingsperiode.beregning

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

        // Grupper dager per yrkesaktivitet
        val dagerPerYrkesaktivitet = input.dagoversikt.groupBy { it.yrkesaktivitetId }

        // Opprett beregning for hver yrkesaktivitet
        val yrkesaktiviteter =
            dagerPerYrkesaktivitet.map { (yrkesaktivitetId, dager) ->
                val dagBeregninger =
                    dager.map { dag ->
                        beregnDag(dag, dagsatsØre, input.refusjon, input.maksdao)
                    }

                YrkesaktivitetBeregning(
                    yrkesaktivitetId = yrkesaktivitetId,
                    dager = dagBeregninger,
                )
            }

        return BeregningData(yrkesaktiviteter = yrkesaktiviteter)
    }

    private fun beregnDag(
        dag: DagoversiktDag,
        dagsatsØre: Long,
        refusjoner: List<RefusjonInput>,
        maksdao: Int,
    ): DagBeregning {
        // Beregn utbetaling basert på grad og dagtype
        val utbetalingØre =
            when (dag.dagtype) {
                "Syk", "SykNav" -> {
                    val grad = dag.grad ?: 100
                    (dagsatsØre * grad) / 100L
                }
                else -> 0L
            }

        // Finn refusjon for denne dagen og yrkesaktivitet
        val refusjonØre =
            refusjoner
                .filter { it.yrkesaktivitetId == dag.yrkesaktivitetId }
                .filter { dag.dato in it.fom..it.tom }
                .sumOf { it.beløpØre }

        return DagBeregning(
            dato = dag.dato,
            utbetalingØre = utbetalingØre,
            refusjonØre = refusjonØre,
        )
    }
}
