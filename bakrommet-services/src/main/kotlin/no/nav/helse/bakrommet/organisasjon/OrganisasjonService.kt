package no.nav.helse.bakrommet.organisasjon

import no.nav.helse.bakrommet.infrastruktur.provider.Organisasjon
import no.nav.helse.bakrommet.infrastruktur.provider.OrganisasjonsnavnProvider

class OrganisasjonService(
    private val organisasjonsnavnProvider: OrganisasjonsnavnProvider,
) {
    suspend fun hentOrganisasjonsnavn(
        orgnummer: String,
    ): Organisasjon = organisasjonsnavnProvider.hentOrganisasjonsnavn(orgnummer)
}
