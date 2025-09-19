package no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet

import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.Beregningssporing
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.Sporbar
import no.nav.helse.dto.ProsentdelDto
import no.nav.helse.økonomi.Prosentdel

val ÅTTI_PROSENT = Prosentdel.gjenopprett(ProsentdelDto(0.8))
val SEKSTIFEM_PROSENT = Prosentdel.gjenopprett(ProsentdelDto(0.65))

/**
 * Extension functions for Yrkesaktivitet
 *
 * Henter dekningsgrad basert på yrkesaktivitetstype og forsikring
 * @return Prosentdel som representerer dekningsgraden
 */
fun Yrkesaktivitet.hentDekningsgrad(): Sporbar<Prosentdel> {
    return when (kategorisering.get("INNTEKTSKATEGORI")) {
        "SELVSTENDIG_NÆRINGSDRIVENDE" -> {
            // Sjekk først om det er en fisker - disse skal ha 100% dekning uansett
            val typeSelvstendig = kategorisering.get("TYPE_SELVSTENDIG_NÆRINGSDRIVENDE")

            if (typeSelvstendig == "FISKER") {
                return Sporbar(Prosentdel.HundreProsent, Beregningssporing.SELVSTENDIG_KOLLEKTIVFORSIKRING_100)
            }

            // Sjekk om det finnes forsikringsinformasjon
            val forsikring = kategorisering["SELVSTENDIG_NÆRINGSDRIVENDE_FORSIKRING"]

            when (forsikring) {
                "FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG" ->
                    Sporbar(
                        Prosentdel.HundreProsent,
                        Beregningssporing.ORDINAER_SELVSTENDIG_NAVFORSIKRING_100,
                    )
                "FORSIKRING_100_PROSENT_FRA_17_SYKEDAG" ->
                    Sporbar(
                        Prosentdel.HundreProsent,
                        Beregningssporing.ORDINAER_SELVSTENDIG_NAVFORSIKRING_100,
                    )
                "FORSIKRING_80_PROSENT_FRA_FØRSTE_SYKEDAG" -> Sporbar(ÅTTI_PROSENT, Beregningssporing.ORDINAER_SELVSTENDIG_80)
                "INGEN_FORSIKRING" -> Sporbar(ÅTTI_PROSENT, Beregningssporing.ORDINAER_SELVSTENDIG_80)
                else -> throw IllegalArgumentException("Ukjent forsikringstype for selvstendig næringsdrivende: $forsikring")
            }
        }
        "INAKTIV" -> {
            // Sjekk variant av inaktiv for å bestemme dekningsgrad
            val variant = kategorisering.get("VARIANT_AV_INAKTIV")

            when (variant) {
                "INAKTIV_VARIANT_A" -> Sporbar(SEKSTIFEM_PROSENT, Beregningssporing.INAKTIV_65)
                "INAKTIV_VARIANT_B" -> Sporbar(Prosentdel.HundreProsent, Beregningssporing.INAKTIV_100)
                else -> throw IllegalArgumentException("Ukjent variant for inaktiv: $variant")
            }
        }
        "ARBEIDSTAKER" -> Sporbar(Prosentdel.HundreProsent, Beregningssporing.ARBEIDSTAKER_100)
        "FRILANSER" -> Sporbar(Prosentdel.HundreProsent, Beregningssporing.FRILANSER_100)
        "ARBEIDSLEDIG" -> Sporbar(Prosentdel.HundreProsent, Beregningssporing.DAGPENGEMOTTAKER_100)
        else -> throw IllegalArgumentException("Ukjent inntektskategori: ${kategorisering.get("INNTEKTSKATEGORI")}")
    }
}
