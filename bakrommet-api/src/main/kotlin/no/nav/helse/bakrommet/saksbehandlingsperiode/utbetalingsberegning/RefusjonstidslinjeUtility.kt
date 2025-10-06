package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagResponse
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.util.UUID
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.beregning.beregnRefusjonstidslinje as beregnRefusjonstidslinjeFunksjonell

/**
 * Legacy wrapper for refusjonstidslinje - bruker nå funksjonell tilnærming
 * @deprecated Bruk beregnRefusjonstidslinje direkte fra beregning-pakken
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
        return beregnRefusjonstidslinjeFunksjonell(sykepengegrunnlag, yrkesaktivitetId, saksbehandlingsperiode)
    }
}
