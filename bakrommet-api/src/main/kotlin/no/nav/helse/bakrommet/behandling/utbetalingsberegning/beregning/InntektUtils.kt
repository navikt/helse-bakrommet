package no.nav.helse.bakrommet.behandling.utbetalingsberegning.beregning

import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.Sykepengegrunnlag
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.Yrkesaktivitet
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.YrkesaktivitetKategorisering.SelvstendigNæringsdrivende
import no.nav.helse.bakrommet.økonomi.tilInntekt
import no.nav.helse.økonomi.Inntekt

/**
 * Finner inntekt for en spesifikk yrkesaktivitet
 */
fun finnInntektForYrkesaktivitet(
    sykepengegrunnlag: Sykepengegrunnlag,
    yrkesaktivitet: Yrkesaktivitet,
): Inntekt? {
    // Hvis næringsdrivende og næringsdel, returner næringsdel.
    if (yrkesaktivitet.kategorisering is SelvstendigNæringsdrivende) {
        sykepengegrunnlag.næringsdel?.let {
            return it.næringsdel.tilInntekt()
        }
    }

    return yrkesaktivitet.inntektData?.omregnetÅrsinntekt?.tilInntekt()
}
