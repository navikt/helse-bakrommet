package no.nav.helse.bakrommet.api.dto.validering

import no.nav.helse.bakrommet.api.dto.interfaces.ApiResponse

data class ValideringDto(
    val id: String,
    val tekst: String,
) : ApiResponse
