package no.nav.helse.bakrommet.behandling.validering

import no.nav.helse.bakrommet.behandling.validering.sjekker.IkkeOppfylt8_2IkkeVurdert8_47

data class SjekkResultat(
    val id: String,
    val tekst: String,
)

internal val alleSjekker: List<ValideringSjekk> = listOf(IkkeOppfylt8_2IkkeVurdert8_47)

internal interface ValideringSjekk {
    fun harInkonsistens(data: ValideringData): Boolean

    val id: String
    val tekst: String
}
