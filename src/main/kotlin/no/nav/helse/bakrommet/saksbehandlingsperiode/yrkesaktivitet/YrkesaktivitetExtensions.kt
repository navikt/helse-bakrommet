package no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet

import no.nav.helse.dto.ProsentdelDto
import no.nav.helse.økonomi.Prosentdel

/**
 * Extension functions for Yrkesaktivitet
 *
 * Henter dekningsgrad basert på yrkesaktivitetstype
 * @return Prosentdel som representerer dekningsgraden
 */
fun Yrkesaktivitet.hentDekningsgrad(): Prosentdel {
    return if (kategorisering.get("INNTEKTSKATEGORI")?.asText() == "SELVSTENDIG_NÆRINGSDRIVENDE") {
        Prosentdel.gjenopprett(ProsentdelDto(0.8))
    } else {
        Prosentdel.HundreProsent
    }
}
