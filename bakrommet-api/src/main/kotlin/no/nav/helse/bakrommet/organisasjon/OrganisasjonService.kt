package no.nav.helse.bakrommet.organisasjon

import no.nav.helse.bakrommet.ereg.EregClient

class OrganisasjonService(
    private val eregClient: EregClient,
) {
    suspend fun hentOrganisasjonsnavn(
        orgnummer: String,
    ): String = eregClient.hentOrganisasjonsnavn(orgnummer)
}
