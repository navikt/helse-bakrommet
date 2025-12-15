package no.nav.helse.bakrommet.kodeverk

annotation class KodeVerksKode(
    val beskrivelse: String,
) // TODO: Unødvendig ?

enum class Vilkårskode {
    @KodeVerksKode("Opptjening i hht 8-2")
    OPPTJENING,

    @KodeVerksKode("8-47")
    INAKTIV,
}
