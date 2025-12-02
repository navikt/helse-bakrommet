package no.nav.helse.bakrommet.api.dto.person

import no.nav.helse.bakrommet.api.dto.interfaces.ApiResponse

data class PersoninfoResponseDto(
    val fødselsnummer: String,
    val aktørId: String,
    val navn: String,
    val alder: Int?,
) : ApiResponse
