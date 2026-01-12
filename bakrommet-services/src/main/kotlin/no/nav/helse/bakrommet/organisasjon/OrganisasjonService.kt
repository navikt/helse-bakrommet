package no.nav.helse.bakrommet.organisasjon

import no.nav.helse.bakrommet.clients.EregProvider
import no.nav.helse.bakrommet.clients.Organisasjon

class OrganisasjonService(
    private val eregClient: EregProvider,
) {
    suspend fun hentOrganisasjonsnavn(
        orgnummer: String,
    ): Organisasjon = eregClient.hentOrganisasjonsnavn(orgnummer)
}
