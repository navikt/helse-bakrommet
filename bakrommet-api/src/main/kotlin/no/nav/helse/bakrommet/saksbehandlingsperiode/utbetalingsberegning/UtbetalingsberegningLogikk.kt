package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.beregning.beregnUtbetalingerForAlleYrkesaktiviteter

/**
 * Legacy wrapper for utbetalingsberegning - bruker nå funksjonell tilnærming
 * @deprecated Bruk beregnUtbetalingerForAlleYrkesaktiviteter direkte
 */
object UtbetalingsberegningLogikk {
    fun beregnAlaSpleis(input: UtbetalingsberegningInput): List<YrkesaktivitetUtbetalingsberegning> {
        return beregnUtbetalingerForAlleYrkesaktiviteter(input)
    }
}
