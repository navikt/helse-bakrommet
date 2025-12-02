package no.nav.helse.bakrommet.organisasjon

import no.nav.helse.bakrommet.ereg.EregClient
import no.nav.helse.bakrommet.ereg.Organisasjon

class OrganisasjonService(
    private val eregClient: EregClient,
) {
    suspend fun hentOrganisasjonsnavn(
        orgnummer: String,
    ): Organisasjon = eregClient.hentOrganisasjonsnavn(orgnummer)
}
