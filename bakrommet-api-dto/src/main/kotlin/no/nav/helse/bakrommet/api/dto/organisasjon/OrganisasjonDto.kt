package no.nav.helse.bakrommet.api.dto.organisasjon

import no.nav.helse.bakrommet.api.dto.interfaces.ApiResponse

data class OrganisasjonDto(
    val navn: String,
    val orgnummer: String,
) : ApiResponse
