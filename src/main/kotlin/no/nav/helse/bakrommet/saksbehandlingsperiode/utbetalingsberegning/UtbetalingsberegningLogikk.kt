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

        // Opprett refusjonstidslinjer for hver yrkesaktivitet
        val refusjonstidslinjer =
            input.yrkesaktivitet.associate { inntektsforhold ->
                inntektsforhold.id to opprettRefusjonstidslinje(input.sykepengegrunnlag, inntektsforhold.id)
            }

        // Samle alle dager fra alle yrkesaktiviteter
        val alleDager = mutableMapOf<LocalDate, MutableList<DagMedYrkesaktivitet>>()

        input.yrkesaktivitet.forEach { inntektsforhold ->
            val dagoversikt = hentDagoversiktFraYrkesaktivitet(inntektsforhold)
            dagoversikt.forEach { dag ->
                alleDager.getOrPut(dag.dato) { mutableListOf() }.add(
                    DagMedYrkesaktivitet(dag, inntektsforhold.id),
                )
            }
        }

        // Beregn dag-for-dag for alle yrkesaktiviteter
        val dagBeregningerPerYrkesaktivitet = mutableMapOf<UUID, MutableList<DagUtbetalingsberegning>>()

        alleDager.forEach { (dato, dagerForDato) ->
            // Opprett økonomi-objekter for alle yrkesaktiviteter denne dagen
            val økonomiList =
                dagerForDato.map { dagMedYrkesaktivitet ->
                    opprettØkonomiForDag(
                        dagMedYrkesaktivitet.dag,
                        input.sykepengegrunnlag,
                        dagMedYrkesaktivitet.yrkesaktivitetId,
                        refusjonstidslinjer[dagMedYrkesaktivitet.yrkesaktivitetId] ?: emptyMap(),
                    )
                }

            // Beregn total sykdomsgrad for alle yrkesaktiviteter denne dagen (som Spleis)
            val økonomiMedTotalGrad = Økonomi.totalSykdomsgrad(økonomiList)

            // Beregn utbetaling for alle yrkesaktiviteter denne dagen sammen
            val beregnedeØkonomier = Økonomi.betal(sykepengegrunnlagBegrenset6G, økonomiMedTotalGrad)

            // Fordel resultatene tilbake til riktig yrkesaktivitet
            dagerForDato.zip(beregnedeØkonomier).forEach { (dagMedYrkesaktivitet, beregnetØkonomi) ->
                val yrkesaktivitetId = dagMedYrkesaktivitet.yrkesaktivitetId
                val dagBeregning = konverterTilDagBeregning(dagMedYrkesaktivitet.dag, beregnetØkonomi)

                dagBeregningerPerYrkesaktivitet.getOrPut(yrkesaktivitetId) { mutableListOf() }.add(dagBeregning)
            }
        }

        // Opprett resultat per yrkesaktivitet
        val yrkesaktiviteter =
            input.yrkesaktivitet.map { inntektsforhold ->
                val dager = dagBeregningerPerYrkesaktivitet[inntektsforhold.id] ?: emptyList()
                YrkesaktivitetUtbetalingsberegning(
                    yrkesaktivitetId = inntektsforhold.id,
                    dager = dager,
                )
            }

        return UtbetalingsberegningData(yrkesaktiviteter = yrkesaktiviteter)
    }

    private data class DagMedYrkesaktivitet(
        val dag: Dag,
        val yrkesaktivitetId: UUID,
    )

    private fun hentDagoversiktFraYrkesaktivitet(yrkesaktivitet: Yrkesaktivitet): List<Dag> {
        return yrkesaktivitet.dagoversikt.tilDagoversikt()
    }

    /**
     * Oppretter en refusjonstidslinje for en yrkesaktivitet basert på refusjonsopplysninger
     * Dette simulerer Spleis sin refusjonstidslinje-funksjonalitet
     */
    private fun opprettRefusjonstidslinje(
        sykepengegrunnlag: SykepengegrunnlagResponse,
        yrkesaktivitetId: UUID,
    ): Map<LocalDate, Inntekt> {
        val refusjonstidslinje = mutableMapOf<LocalDate, Inntekt>()

        sykepengegrunnlag.inntekter
            .filter { it.yrkesaktivitetId == yrkesaktivitetId }
            .flatMap { inntekt ->
                inntekt.refusjon.map { refusjon ->
                    // Fyll tidslinjen for hver dag i refusjonsperioden
                    refusjon.fom.datesUntil(refusjon.tom.plusDays(1)).forEach { dato ->
                        val beløp = Inntekt.gjenopprett(InntektbeløpDto.DagligInt(refusjon.beløpØre.toInt()))
                        refusjonstidslinje[dato] = beløp
                    }
                }
            }

        return refusjonstidslinje
    }

    private fun opprettØkonomiForDag(
        dag: Dag,
        sykepengegrunnlag: SykepengegrunnlagResponse,
        yrkesaktivitetId: UUID,
        refusjonstidslinje: Map<LocalDate, Inntekt>,
    ): Økonomi {
        // Finn inntekt for denne inntektsforholdet
        val inntektForYrkesaktivitet = finnInntektForYrkesaktivitet(sykepengegrunnlag, yrkesaktivitetId)
        val aktuellDagsinntekt =
            Inntekt.gjenopprett(
                InntektbeløpDto.MånedligDouble(inntektForYrkesaktivitet.beløpPerMånedØre.toDouble()),
            )

        // Hent refusjonsbeløp fra refusjonstidslinje (som Spleis)
        val refusjonsbeløp = refusjonstidslinje[dag.dato] ?: Inntekt.INGEN

        // Opprett økonomi-objekt basert på dagtype og grad
        return when (dag.dagtype) {
            Dagtype.Syk, Dagtype.SykNav -> {
                val sykdomsgrad = Prosentdel.gjenopprett(ProsentdelDto((dag.grad ?: 100) / 100.0))

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
    }

    private fun konverterTilDagBeregning(
        dag: Dag,
        beregnetØkonomi: Økonomi,
    ): DagUtbetalingsberegning {
        // Konverter tilbake til øre-format for output
        // dagligInt returnerer kroner som Int, men vi trenger øre
        val utbetalingØre = ((beregnetØkonomi.personbeløp?.dagligInt ?: 0) * 100).toLong()
        val refusjonØre = ((beregnetØkonomi.arbeidsgiverbeløp?.dagligInt ?: 0) * 100).toLong()

        // Hent total grad som heltall (som Spleis)
        val totalGrad = beregnetØkonomi.brukTotalGrad { it }

        return DagUtbetalingsberegning(
            dato = dag.dato,
            utbetalingØre = utbetalingØre,
            refusjonØre = refusjonØre,
            totalGrad = totalGrad,
        )
    }

    private fun finnInntektForYrkesaktivitet(
        sykepengegrunnlag: SykepengegrunnlagResponse,
        yrkesaktivitetId: UUID,
    ): no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.Inntekt {
        return sykepengegrunnlag.inntekter.find { it.yrkesaktivitetId == yrkesaktivitetId }
            ?: throw IllegalArgumentException("Fant ikke inntekt for inntektsforhold $yrkesaktivitetId")
    }
}
