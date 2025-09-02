package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dag
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dagtype
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.tilDagoversikt
import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagResponse
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.Yrkesaktivitet
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.hentDekningsgrad
import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate
import java.util.UUID

/**
 * Pure function utility for utbetalingsberegning
 * Alle funksjoner er stateless og har ingen sideeffekter
 */
object UtbetalingsberegningLogikk {
    /**
     * Beregner sykepengegrunnlaget basert på input data ved hjelp av økonomi-klassene
     *
     * @param input Input data for beregningen
     * @return BeregningData med resultatet
     */
    fun beregn(input: UtbetalingsberegningInput): UtbetalingsberegningData {
        val sykepengegrunnlagBegrenset6G = opprettSykepengegrunnlag(input.sykepengegrunnlag)
        val refusjonstidslinjer = opprettRefusjonstidslinjer(input)
        val alleDager = samleAlleDager(input, refusjonstidslinjer)
        val dagBeregninger = beregnDagForDag(alleDager, sykepengegrunnlagBegrenset6G, input.sykepengegrunnlag, refusjonstidslinjer)

        return opprettResultat(input.yrkesaktivitet, dagBeregninger)
    }

    private fun opprettSykepengegrunnlag(sykepengegrunnlag: SykepengegrunnlagResponse): Inntekt {
        return Inntekt.gjenopprett(
            InntektbeløpDto.Årlig(sykepengegrunnlag.sykepengegrunnlagØre / UtbetalingsberegningKonfigurasjon.ØRE_TIL_KRONER_FAKTOR),
        )
    }

    private fun opprettRefusjonstidslinjer(input: UtbetalingsberegningInput): Map<UUID, Map<LocalDate, Inntekt>> {
        return input.yrkesaktivitet.associate { yrkesaktivitet ->
            yrkesaktivitet.id to
                RefusjonstidslinjeUtility.beregnRefusjonstidslinje(
                    input.sykepengegrunnlag,
                    yrkesaktivitet.id,
                    input.saksbehandlingsperiode,
                )
        }
    }

    private fun samleAlleDager(
        input: UtbetalingsberegningInput,
        refusjonstidslinjer: Map<UUID, Map<LocalDate, Inntekt>>,
    ): Map<LocalDate, List<DagMedYrkesaktivitet>> {
        val alleDager = mutableMapOf<LocalDate, MutableList<DagMedYrkesaktivitet>>()

        input.yrkesaktivitet.forEach { yrkesaktivitet ->
            val dagoversikt = hentDagoversiktFraYrkesaktivitet(yrkesaktivitet)
            val komplettDagoversikt = fyllUtManglendeDager(dagoversikt, input.saksbehandlingsperiode)

            komplettDagoversikt.forEach { dag ->
                alleDager.getOrPut(dag.dato) { mutableListOf() }.add(
                    DagMedYrkesaktivitet(dag, yrkesaktivitet),
                )
            }
        }

        return alleDager
    }

    private fun beregnDagForDag(
        alleDager: Map<LocalDate, List<DagMedYrkesaktivitet>>,
        sykepengegrunnlagBegrenset6G: Inntekt,
        sykepengegrunnlag: SykepengegrunnlagResponse,
        refusjonstidslinjer: Map<UUID, Map<LocalDate, Inntekt>>,
    ): Map<UUID, List<DagUtbetalingsberegning>> {
        val dagBeregningerPerYrkesaktivitet = mutableMapOf<UUID, MutableList<DagUtbetalingsberegning>>()

        alleDager.forEach { (dato, dagerForDato) ->
            val økonomiList =
                dagerForDato.map { dagMedYrkesaktivitet ->
                    beregnØkonomiForDag(
                        dagMedYrkesaktivitet.dag,
                        sykepengegrunnlag,
                        refusjonstidslinjer[dagMedYrkesaktivitet.yrkesaktivitet.id] ?: emptyMap(),
                        dagMedYrkesaktivitet.yrkesaktivitet,
                    )
                }

            val økonomiMedTotalGrad = Økonomi.totalSykdomsgrad(økonomiList)
            val beregnedeØkonomier =
                try {
                    Økonomi.betal(sykepengegrunnlagBegrenset6G, økonomiMedTotalGrad)
                } catch (e: IllegalStateException) {
                    // Hvis Økonomi.betal() feiler på grunn av restbeløp, bruk original økonomi
                    økonomiMedTotalGrad
                }

            dagerForDato.zip(beregnedeØkonomier).forEach { (dagMedYrkesaktivitet, beregnetØkonomi) ->
                val yrkesaktivitetId = dagMedYrkesaktivitet.yrkesaktivitet.id
                val dagBeregning = konverterTilDagBeregning(dagMedYrkesaktivitet.dag, beregnetØkonomi, dagMedYrkesaktivitet.yrkesaktivitet)
                dagBeregningerPerYrkesaktivitet.getOrPut(yrkesaktivitetId) { mutableListOf() }.add(dagBeregning)
            }
        }

        return dagBeregningerPerYrkesaktivitet
    }

    private fun hentDagoversiktFraYrkesaktivitet(yrkesaktivitet: Yrkesaktivitet): List<Dag> {
        return yrkesaktivitet.dagoversikt.tilDagoversikt()
    }

    private fun fyllUtManglendeDager(
        eksisterendeDager: List<Dag>,
        saksbehandlingsperiode: Saksbehandlingsperiode,
    ): List<Dag> {
        validerPeriode(saksbehandlingsperiode)

        val eksisterendeDatoer = eksisterendeDager.map { it.dato }.toSet()
        val komplettDagoversikt = mutableListOf<Dag>()

        // Legg til alle eksisterende dager
        komplettDagoversikt.addAll(eksisterendeDager)

        // Fyll ut manglende dager som arbeidsdager
        var aktuellDato = saksbehandlingsperiode.fom
        while (!aktuellDato.isAfter(saksbehandlingsperiode.tom)) {
            if (!eksisterendeDatoer.contains(aktuellDato)) {
                val arbeidsdag = opprettArbeidsdag(aktuellDato)
                komplettDagoversikt.add(arbeidsdag)
            }
            aktuellDato = aktuellDato.plusDays(1)
        }

        // Sorter dager etter dato
        return komplettDagoversikt.sortedBy { it.dato }
    }

    private fun validerPeriode(saksbehandlingsperiode: Saksbehandlingsperiode) {
        if (saksbehandlingsperiode.fom.isAfter(saksbehandlingsperiode.tom)) {
            throw UtbetalingsberegningFeil.UgyldigPeriode(
                saksbehandlingsperiode.fom,
                saksbehandlingsperiode.tom,
            )
        }
    }

    private fun opprettArbeidsdag(dato: LocalDate): Dag {
        return Dag(
            dato = dato,
            dagtype = Dagtype.Arbeidsdag,
            grad = null,
            avvistBegrunnelse = emptyList(),
            kilde = null,
        )
    }

    private fun beregnØkonomiForDag(
        dag: Dag,
        sykepengegrunnlag: SykepengegrunnlagResponse,
        refusjonstidslinje: Map<LocalDate, Inntekt>,
        yrkesaktivitet: Yrkesaktivitet,
    ): Økonomi {
        val aktuellDagsinntekt = finnInntektForYrkesaktivitet(sykepengegrunnlag, yrkesaktivitet.id)
        val refusjonsbeløp = refusjonstidslinje[dag.dato] ?: Inntekt.INGEN
        val sykdomsgrad = Sykdomsgrad(dag.grad ?: 0).tilProsentdel()

        val dekningsgrad = yrkesaktivitet.hentDekningsgrad()

        return Økonomi.inntekt(
            sykdomsgrad = sykdomsgrad,
            aktuellDagsinntekt = aktuellDagsinntekt,
            dekningsgrad = dekningsgrad,
            refusjonsbeløp = refusjonsbeløp,
            inntektjustering = Inntekt.INGEN,
        )
    }

    private fun finnInntektForYrkesaktivitet(
        sykepengegrunnlag: SykepengegrunnlagResponse,
        yrkesaktivitetId: UUID,
    ): Inntekt {
        val inntekt =
            sykepengegrunnlag.inntekter.find { it.yrkesaktivitetId == yrkesaktivitetId }
                ?: throw UtbetalingsberegningFeil.ManglendeInntekt(yrkesaktivitetId)

        return Inntekt.gjenopprett(
            InntektbeløpDto.Årlig(
                inntekt.beløpPerMånedØre * UtbetalingsberegningKonfigurasjon.MÅNEDLIG_TIL_ÅRLIG_FAKTOR / UtbetalingsberegningKonfigurasjon.ØRE_TIL_KRONER_FAKTOR,
            ),
        )
    }

    private fun konverterTilDagBeregning(
        dag: Dag,
        beregnetØkonomi: Økonomi,
        yrkesaktivitet: Yrkesaktivitet,
    ): DagUtbetalingsberegning {
        // Konverter tilbake til øre-format for output
        // dagligInt returnerer kroner som Int, men vi trenger øre
        val utbetalingØre = ((beregnetØkonomi.personbeløp?.dagligInt ?: 0) * UtbetalingsberegningKonfigurasjon.ØRE_TIL_KRONER_FAKTOR).toLong()
        val refusjonØre = ((beregnetØkonomi.arbeidsgiverbeløp?.dagligInt ?: 0) * UtbetalingsberegningKonfigurasjon.ØRE_TIL_KRONER_FAKTOR).toLong()

        // Hent total grad som heltall (som Spleis)
        val totalGrad = beregnetØkonomi.brukTotalGrad { it }

        // Hent dekningsgrad fra yrkesaktivitet
        val dekningsgrad = yrkesaktivitet.hentDekningsgrad().toDouble()

        return DagUtbetalingsberegning(
            dato = dag.dato,
            utbetalingØre = utbetalingØre,
            refusjonØre = refusjonØre,
            totalGrad = totalGrad,
            dekningsgrad = dekningsgrad,
        )
    }

    private fun opprettResultat(
        yrkesaktiviteter: List<Yrkesaktivitet>,
        dagBeregninger: Map<UUID, List<DagUtbetalingsberegning>>,
    ): UtbetalingsberegningData {
        val yrkesaktivitetUtbetalingsberegninger =
            yrkesaktiviteter.map { yrkesaktivitet ->
                val dager = dagBeregninger[yrkesaktivitet.id] ?: emptyList()
                YrkesaktivitetUtbetalingsberegning(
                    yrkesaktivitetId = yrkesaktivitet.id,
                    dager = dager,
                )
            }

        return UtbetalingsberegningData(yrkesaktivitetUtbetalingsberegninger)
    }

    private data class DagMedYrkesaktivitet(
        val dag: Dag,
        val yrkesaktivitet: Yrkesaktivitet,
    )
}
