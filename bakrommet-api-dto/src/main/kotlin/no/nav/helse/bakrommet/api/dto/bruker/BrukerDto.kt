package no.nav.helse.bakrommet.api.dto.bruker

import no.nav.helse.bakrommet.api.dto.interfaces.ApiResponse

data class BrukerDto(
    val navn: String,
    val navIdent: String,
    val preferredUsername: String,
    val roller: Set<RolleDto>,
) : ApiResponse
