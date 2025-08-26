package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dag
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dagtype
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.tilDagoversikt
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagResponse
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.Yrkesaktivitet
import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.dto.ProsentdelDto
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate
import java.util.UUID

object UtbetalingsberegningLogikk {
    /**
     * Beregner sykepengegrunnlaget basert på input data ved hjelp av økonomi-klassene
     *
     * @param input Input data for beregningen
     * @return BeregningData med resultatet
     */
    fun beregn(input: UtbetalingsberegningInput): UtbetalingsberegningData {
        // Konverter sykepengegrunnlag til Inntekt-objekt (øre til daglig inntekt)
        val sykepengegrunnlagBegrenset6G =
            Inntekt.gjenopprett(
                InntektbeløpDto.Årlig(input.sykepengegrunnlag.sykepengegrunnlagØre / 100.0),
            )

        // Opprett beregning for hver inntektsforhold
        val yrkesaktiviteter =
            input.yrkesaktivitet.map { inntektsforhold ->
                val dagoversikt = hentDagoversiktFraYrkesaktivitet(inntektsforhold)
                val dagBeregninger =
                    dagoversikt.map { dag ->
                        beregnDagMedØkonomi(
                            dag,
                            sykepengegrunnlagBegrenset6G,
                            input.sykepengegrunnlag,
                            inntektsforhold.id,
                        )
                    }

                YrkesaktivitetUtbetalingsberegning(
                    yrkesaktivitetId = inntektsforhold.id,
                    dager = dagBeregninger,
                )
            }

        return UtbetalingsberegningData(yrkesaktiviteter = yrkesaktiviteter)
    }

    private fun hentDagoversiktFraYrkesaktivitet(yrkesaktivitet: Yrkesaktivitet): List<Dag> {
        return yrkesaktivitet.dagoversikt.tilDagoversikt()
    }

    private fun beregnDagMedØkonomi(
        dag: Dag,
        sykepengegrunnlagBegrenset6G: Inntekt,
        sykepengegrunnlag: SykepengegrunnlagResponse,
        yrkesaktivitetId: UUID,
    ): DagUtbetalingsberegning {
        // Finn inntekt for denne inntektsforholdet
        val inntektForYrkesaktivitet = finnInntektForYrkesaktivitet(sykepengegrunnlag, yrkesaktivitetId)
        val aktuellDagsinntekt =
            Inntekt.gjenopprett(
                InntektbeløpDto.MånedligDouble(inntektForYrkesaktivitet.beløpPerMånedØre.toDouble()),
            )

        // Opprett økonomi-objekt basert på dagtype og grad
        val økonomi =
            when (dag.dagtype) {
                Dagtype.Syk, Dagtype.SykNav -> {
                    val sykdomsgrad = Prosentdel.gjenopprett(ProsentdelDto((dag.grad ?: 100) / 100.0))
                    val refusjonØre = finnRefusjonForDag(dag.dato, sykepengegrunnlag, yrkesaktivitetId)
                    val refusjonsbeløp =
                        if (refusjonØre > 0) {
                            Inntekt.gjenopprett(InntektbeløpDto.DagligInt(refusjonØre.toInt()))
                        } else {
                            Inntekt.INGEN
                        }

                    Økonomi.inntekt(
                        sykdomsgrad = sykdomsgrad,
                        aktuellDagsinntekt = aktuellDagsinntekt,
                        dekningsgrad = Prosentdel.gjenopprett(ProsentdelDto(1.0)),
                        refusjonsbeløp = refusjonsbeløp,
                        inntektjustering = Inntekt.INGEN,
                    )
                }

                else -> {
                    // For dager som ikke skal betales (Arbeidsdag, Helg, Ferie, etc.)
                    Økonomi.ikkeBetalt(
                        aktuellDagsinntekt = aktuellDagsinntekt,
                        inntektjustering = Inntekt.INGEN,
                    )
                }
            }

        // Beregn utbetaling ved hjelp av økonomi-klassene
        val beregnetØkonomi = Økonomi.betal(sykepengegrunnlagBegrenset6G, listOf(økonomi)).first()

        // Konverter tilbake til øre-format for output
        val utbetalingØre = (beregnetØkonomi.personbeløp?.dagligInt ?: 0).toLong()
        val refusjonØre = (beregnetØkonomi.arbeidsgiverbeløp?.dagligInt ?: 0).toLong()

        return DagUtbetalingsberegning(
            dato = dag.dato,
            utbetalingØre = utbetalingØre,
            refusjonØre = refusjonØre,
        )
    }

    private fun finnInntektForYrkesaktivitet(
        sykepengegrunnlag: SykepengegrunnlagResponse,
        yrkesaktivitetId: UUID,
    ): no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.Inntekt {
        return sykepengegrunnlag.inntekter.find { it.yrkesaktivitetId == yrkesaktivitetId }
            ?: throw IllegalArgumentException("Fant ikke inntekt for inntektsforhold $yrkesaktivitetId")
    }

    private fun finnRefusjonForDag(
        dato: LocalDate,
        sykepengegrunnlag: SykepengegrunnlagResponse,
        yrkesaktivitetId: UUID,
    ): Long {
        // Finn refusjon fra sykepengegrunnlaget for denne datoen og inntektsforholdet
        return sykepengegrunnlag.inntekter
            .filter { it.yrkesaktivitetId == yrkesaktivitetId }
            .flatMap { inntekt ->
                inntekt.refusjon.filter { refusjon ->
                    dato in refusjon.fom..refusjon.tom
                }
            }
            .sumOf { it.beløpØre }
    }
}
