package no.nav.helse.bakrommet.api.dto.person

import no.nav.helse.bakrommet.api.dto.interfaces.ApiResponse

data class PersonsøkResponseDto(
    val personId: String,
) : ApiResponse
