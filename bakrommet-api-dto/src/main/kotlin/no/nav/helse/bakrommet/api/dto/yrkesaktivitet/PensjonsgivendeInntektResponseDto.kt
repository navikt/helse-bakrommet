package no.nav.helse.bakrommet.api.dto.yrkesaktivitet

import no.nav.helse.bakrommet.api.dto.interfaces.ApiResponse

sealed class PensjonsgivendeInntektResponseDto : ApiResponse {
    data class Suksess(
        val success: Boolean = true,
        val data: InntektDataDto,
    ) : PensjonsgivendeInntektResponseDto()

    data class Feil(
        val success: Boolean = false,
        val feilmelding: String,
    ) : PensjonsgivendeInntektResponseDto()
}
