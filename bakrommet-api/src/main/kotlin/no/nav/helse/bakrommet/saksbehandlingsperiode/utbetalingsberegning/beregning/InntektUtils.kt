package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.beregning

import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.Sykepengegrunnlag
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.Yrkesaktivitet
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.YrkesaktivitetKategorisering.SelvstendigNæringsdrivende
import no.nav.helse.bakrommet.økonomi.tilInntekt
import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.økonomi.Inntekt

/**
 * Finner inntekt for en spesifikk yrkesaktivitet
 */
fun finnInntektForYrkesaktivitet(
    sykepengegrunnlag: Sykepengegrunnlag,
    yrkesaktivitet: Yrkesaktivitet,
): Inntekt {
    // Hvis næringsdrivende og næringsdel, returner næringsdel.
    if(yrkesaktivitet.kategorisering is SelvstendigNæringsdrivende){
        sykepengegrunnlag.næringsdel?.let {
            return it.næringsdel.tilInntekt()
        }
    }
    requireNotNull(yrkesaktivitet.inntektData)
    return yrkesaktivitet.inntektData.omregnetÅrsinntekt.tilInntekt()
}

