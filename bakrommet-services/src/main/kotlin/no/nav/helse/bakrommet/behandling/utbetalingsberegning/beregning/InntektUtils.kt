package no.nav.helse.bakrommet.behandling.utbetalingsberegning.beregning

import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.Sykepengegrunnlag
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.SykepengegrunnlagBase
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.LegacyYrkesaktivitet
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.YrkesaktivitetKategorisering.SelvstendigNæringsdrivende
import no.nav.helse.bakrommet.økonomi.tilInntekt
import no.nav.helse.økonomi.Inntekt

/**
 * Finner inntekt for en spesifikk yrkesaktivitet
 */
fun finnInntektForYrkesaktivitet(
    sykepengegrunnlag: SykepengegrunnlagBase,
    legacyYrkesaktivitet: LegacyYrkesaktivitet,
): Inntekt? {
    // Hvis næringsdrivende og næringsdel, returner næringsdel.
    if (legacyYrkesaktivitet.kategorisering is SelvstendigNæringsdrivende && sykepengegrunnlag is Sykepengegrunnlag) {
        sykepengegrunnlag.næringsdel?.let {
            return it.næringsdel.tilInntekt()
        }
    }

    return legacyYrkesaktivitet.inntektData?.omregnetÅrsinntekt?.tilInntekt()
}
