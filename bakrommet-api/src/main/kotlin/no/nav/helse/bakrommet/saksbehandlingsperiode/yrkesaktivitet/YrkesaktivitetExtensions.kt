package no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet

import no.nav.helse.bakrommet.BeregningskoderDekningsgrad
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
fun YrkesaktivitetDbRecord.hentDekningsgrad(): Sporbar<ProsentdelDto> = kategorisering.hentDekningsgrad()

/**
 * Type-sikker versjon av hentDekningsgrad
 */
fun YrkesaktivitetKategorisering.hentDekningsgrad(): Sporbar<ProsentdelDto> =
    when (this) {
        is YrkesaktivitetKategorisering.SelvstendigNæringsdrivende -> {
            when (val typeSelvstendig = this.typeSelvstendigNæringsdrivende) {
                is TypeSelvstendigNæringsdrivende.Fisker -> {
                    Sporbar(HUNDRE_PROSENT, BeregningskoderDekningsgrad.SELVSTENDIG_KOLLEKTIVFORSIKRING_DEKNINGSGRAD_100)
                }
                else -> {
                    when (typeSelvstendig.forsikring) {
                        SelvstendigForsikring.FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG ->
                            Sporbar(HUNDRE_PROSENT, BeregningskoderDekningsgrad.ORDINAER_SELVSTENDIG_NAVFORSIKRING_DEKNINGSGRAD_100)
                        SelvstendigForsikring.FORSIKRING_100_PROSENT_FRA_17_SYKEDAG ->
                            Sporbar(HUNDRE_PROSENT, BeregningskoderDekningsgrad.ORDINAER_SELVSTENDIG_NAVFORSIKRING_DEKNINGSGRAD_100)
                        SelvstendigForsikring.FORSIKRING_80_PROSENT_FRA_FØRSTE_SYKEDAG ->
                            Sporbar(ÅTTI_PROSENT, BeregningskoderDekningsgrad.ORDINAER_SELVSTENDIG_DEKNINGSGRAD_80)
                        SelvstendigForsikring.INGEN_FORSIKRING ->
                            Sporbar(ÅTTI_PROSENT, BeregningskoderDekningsgrad.ORDINAER_SELVSTENDIG_DEKNINGSGRAD_80)
                    }
                }
            }
        }
        is YrkesaktivitetKategorisering.Inaktiv -> {
            when (this.variant) {
                VariantAvInaktiv.INAKTIV_VARIANT_A -> Sporbar(SEKSTIFEM_PROSENT, BeregningskoderDekningsgrad.INAKTIV_DEKNINGSGRAD_65)
                VariantAvInaktiv.INAKTIV_VARIANT_B -> Sporbar(HUNDRE_PROSENT, BeregningskoderDekningsgrad.INAKTIV_DEKNINGSGRAD_100)
            }
        }
        is YrkesaktivitetKategorisering.Arbeidstaker -> Sporbar(HUNDRE_PROSENT, BeregningskoderDekningsgrad.ARBEIDSTAKER_DEKNINGSGRAD_100)
        is YrkesaktivitetKategorisering.Frilanser -> Sporbar(HUNDRE_PROSENT, BeregningskoderDekningsgrad.FRILANSER_DEKNINGSGRAD_100)
        is YrkesaktivitetKategorisering.Arbeidsledig -> Sporbar(HUNDRE_PROSENT, BeregningskoderDekningsgrad.ARBEIDSLEDIG_DEKNINGSGRAD_100)
    }
