package no.nav.helse.bakrommet.infrastruktur.provider

data class Organisasjon(
    val navn: String,
    val orgnummer: String,
)

interface OrganisasjonsnavnProvider {
    suspend fun hentOrganisasjonsnavn(
        orgnummer: String,
    ): Organisasjon
}
