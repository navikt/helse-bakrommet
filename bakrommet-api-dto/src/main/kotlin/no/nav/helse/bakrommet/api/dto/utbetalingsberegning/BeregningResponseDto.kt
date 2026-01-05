package no.nav.helse.bakrommet.api.dto.utbetalingsberegning

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.helse.bakrommet.api.dto.interfaces.ApiResponse
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class BeregningResponseDto(
    val id: UUID,
    val behandlingId: UUID,
    val beregningData: BeregningDataDto,
    val opprettet: String,
    val opprettetAv: String,
    val sistOppdatert: String,
) : ApiResponse
