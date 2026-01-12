package no.nav.helse.bakrommet.clients

data class Organisasjon(
    val navn: String,
    val orgnummer: String,
)

interface EregProvider {
    suspend fun hentOrganisasjonsnavn(
        orgnummer: String,
    ): Organisasjon
}
