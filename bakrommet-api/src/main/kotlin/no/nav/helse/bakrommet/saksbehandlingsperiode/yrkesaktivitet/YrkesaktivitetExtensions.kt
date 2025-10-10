package no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet

import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.Beregningssporing
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.Sporbar
import no.nav.helse.dto.ProsentdelDto

val HUNDRE_PROSENT = ProsentdelDto(1.0)
val ÅTTI_PROSENT = ProsentdelDto(0.8)
val SEKSTIFEM_PROSENT = ProsentdelDto(0.65)

/**
 * Extension functions for Yrkesaktivitet
 *
 * Henter dekningsgrad basert på yrkesaktivitetstype og forsikring
 * @return Prosentdel som representerer dekningsgraden
 */
fun Yrkesaktivitet.hentDekningsgrad(): Sporbar<ProsentdelDto> {
    // Konverter til type-sikker sealed class for lettere håndtering
    val typeSikkerKategorisering = YrkesaktivitetKategoriseringMapper.fromMap(kategorisering)
    return typeSikkerKategorisering.hentDekningsgrad()
}

/**
 * Type-sikker versjon av hentDekningsgrad
 */
fun YrkesaktivitetKategorisering.hentDekningsgrad(): Sporbar<ProsentdelDto> =
    when (this) {
        is YrkesaktivitetKategorisering.SelvstendigNæringsdrivende -> {
            when (val typeSelvstendig = this.type) {
                is TypeSelvstendigNæringsdrivende.Fisker -> {
                    Sporbar(HUNDRE_PROSENT, Beregningssporing.SELVSTENDIG_KOLLEKTIVFORSIKRING_100)
                }
                else -> {
                    when (typeSelvstendig.forsikring) {
                        SelvstendigForsikring.FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG ->
                            Sporbar(HUNDRE_PROSENT, Beregningssporing.ORDINAER_SELVSTENDIG_NAVFORSIKRING_100)
                        SelvstendigForsikring.FORSIKRING_100_PROSENT_FRA_17_SYKEDAG ->
                            Sporbar(HUNDRE_PROSENT, Beregningssporing.ORDINAER_SELVSTENDIG_NAVFORSIKRING_100)
                        SelvstendigForsikring.FORSIKRING_80_PROSENT_FRA_FØRSTE_SYKEDAG ->
                            Sporbar(ÅTTI_PROSENT, Beregningssporing.ORDINAER_SELVSTENDIG_80)
                        SelvstendigForsikring.INGEN_FORSIKRING ->
                            Sporbar(ÅTTI_PROSENT, Beregningssporing.ORDINAER_SELVSTENDIG_80)
                    }
                }
            }
        }
        is YrkesaktivitetKategorisering.Inaktiv -> {
            when (this.variant) {
                VariantAvInaktiv.INAKTIV_VARIANT_A -> Sporbar(SEKSTIFEM_PROSENT, Beregningssporing.INAKTIV_65)
                VariantAvInaktiv.INAKTIV_VARIANT_B -> Sporbar(HUNDRE_PROSENT, Beregningssporing.INAKTIV_100)
            }
        }
        is YrkesaktivitetKategorisering.Arbeidstaker -> Sporbar(HUNDRE_PROSENT, Beregningssporing.ARBEIDSTAKER_100)
        is YrkesaktivitetKategorisering.Frilanser -> Sporbar(HUNDRE_PROSENT, Beregningssporing.FRILANSER_100)
        is YrkesaktivitetKategorisering.Arbeidsledig -> Sporbar(HUNDRE_PROSENT, Beregningssporing.DAGPENGEMOTTAKER_100)
    }
