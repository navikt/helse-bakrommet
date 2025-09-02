package no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet

import no.nav.helse.dto.ProsentdelDto
import no.nav.helse.økonomi.Prosentdel

/**
 * Extension functions for Yrkesaktivitet
 *
 * Henter dekningsgrad basert på yrkesaktivitetstype og forsikring
 * @return Prosentdel som representerer dekningsgraden
 */
fun Yrkesaktivitet.hentDekningsgrad(): Prosentdel {
    return when (kategorisering.get("INNTEKTSKATEGORI")?.asText()) {
        "SELVSTENDIG_NÆRINGSDRIVENDE" -> {
            // Sjekk om det finnes forsikringsinformasjon
            val forsikring = kategorisering.get("SELVSTENDIG_NÆRINGSDRIVENDE_FORSIKRING")?.asText()

            when (forsikring) {
                "FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG" -> Prosentdel.HundreProsent
                "FORSIKRING_100_PROSENT_FRA_17_SYKEDAG" -> Prosentdel.HundreProsent
                "FORSIKRING_80_PROSENT_FRA_FØRSTE_SYKEDAG" -> Prosentdel.gjenopprett(ProsentdelDto(0.8))
                "INGEN_FORSIKRING" -> Prosentdel.gjenopprett(ProsentdelDto(0.8))
                else -> Prosentdel.gjenopprett(ProsentdelDto(0.8)) // Standard for næringsdrivende
            }
        }
        "INAKTIV" -> {
            // Sjekk variant av inaktiv for å bestemme dekningsgrad
            val variant = kategorisering.get("VARIANT_AV_INAKTIV")?.asText()

            when (variant) {
                "INAKTIV_VARIANT_A" -> Prosentdel.gjenopprett(ProsentdelDto(0.65)) // 65% dekningsgrad
                "INAKTIV_VARIANT_B" -> Prosentdel.HundreProsent // 100% dekningsgrad
                else -> Prosentdel.HundreProsent // Standard for inaktive
            }
        }
        else -> Prosentdel.HundreProsent // Standard for andre yrkesaktiviteter
    }
}
