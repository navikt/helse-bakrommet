package no.nav.helse.bakrommet.behandling.validering

data class SjekkResultat(
    val melding: String,
)

internal val alleSjekker: List<ValideringSjekk> = listOf()

internal interface ValideringSjekk {
    fun sjekk(data: ValideringData): SjekkResultat
}
