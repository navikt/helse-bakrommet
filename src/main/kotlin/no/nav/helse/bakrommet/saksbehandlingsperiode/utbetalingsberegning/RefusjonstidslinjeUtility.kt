package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagResponse
import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.util.UUID

/**
 * Utility-klasse for å håndtere refusjonsuttrekning og tidslinje-opprettelse
 */
object RefusjonstidslinjeUtility {
    /**
     * Beregner refusjonstidslinje for en spesifikk yrkesaktivitet
     *
     * @param sykepengegrunnlag Sykepengegrunnlaget som inneholder refusjonsinformasjon
     * @param yrkesaktivitetId ID til yrkesaktiviteten
     * @param saksbehandlingsperiode Saksbehandlingsperioden for å håndtere åpne refusjonsperioder
     * @return Map over datoer og tilhørende refusjonsbeløp
     */
    fun beregnRefusjonstidslinje(
        sykepengegrunnlag: SykepengegrunnlagResponse,
        yrkesaktivitetId: UUID,
        saksbehandlingsperiode: Saksbehandlingsperiode,
    ): Map<LocalDate, Inntekt> {
        val refusjonstidslinje = mutableMapOf<LocalDate, Inntekt>()

        sykepengegrunnlag.inntekter
            .filter { it.yrkesaktivitetId == yrkesaktivitetId }
            .flatMap { inntekt ->
                inntekt.refusjon.map { refusjon ->
                    // Fyll tidslinjen for hver dag i refusjonsperioden
                    // Hvis tom er null, bruk periodens tom (saksbehandlingsperioden.tom)
                    val refusjonTom = refusjon.tom ?: saksbehandlingsperiode.tom
                    refusjon.fom.datesUntil(refusjonTom.plusDays(1)).forEach { dato ->
                        val dagligBeløpKroner = konverterMånedligØreTilDagligKroner(refusjon.beløpØre)
                        val beløp = Inntekt.gjenopprett(InntektbeløpDto.DagligInt(dagligBeløpKroner))
                        refusjonstidslinje[dato] = beløp
                    }
                }
            }

        return refusjonstidslinje
    }

    /**
     * Konverterer månedlig beløp i øre til daglig beløp i kroner
     *
     * @param månedligBeløpØre Beløp per måned i øre
     * @return Daglig beløp i kroner
     */
    private fun konverterMånedligØreTilDagligKroner(månedligBeløpØre: Long): Int {
        // Konverter fra månedlig øre til daglig kroner (gange med 12 og dele på 260 arbeidsdager)
        return (
            (månedligBeløpØre * UtbetalingsberegningKonfigurasjon.MÅNEDLIG_TIL_ÅRLIG_FAKTOR) /
                UtbetalingsberegningKonfigurasjon.STANDARD_ÅRLIGE_ARBEIDSDAGER /
                UtbetalingsberegningKonfigurasjon.ØRE_TIL_KRONER_FAKTOR
        ).toInt()
    }
}
