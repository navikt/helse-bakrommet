package no.nav.helse.bakrommet.behandling.validering

import no.nav.helse.bakrommet.behandling.validering.sjekker.AvslåttBegrunnelseUtenVilkårsvurdering
import no.nav.helse.bakrommet.behandling.validering.sjekker.IkkeOppfylt8_2IkkeVurdert8_47
import no.nav.helse.bakrommet.behandling.validering.sjekker.Oppfylt8_47IkkeVurdert8_47_Inntektstap

data class SjekkResultat(
    val id: String,
    val tekst: String,
)

internal val alleSjekker: List<ValideringSjekk> =
    listOf(
        IkkeOppfylt8_2IkkeVurdert8_47,
        AvslåttBegrunnelseUtenVilkårsvurdering,
        Oppfylt8_47IkkeVurdert8_47_Inntektstap,
    )

internal interface ValideringSjekk {
    fun harInkonsistens(data: ValideringData): Boolean

    val id: String
    val tekst: String
}
