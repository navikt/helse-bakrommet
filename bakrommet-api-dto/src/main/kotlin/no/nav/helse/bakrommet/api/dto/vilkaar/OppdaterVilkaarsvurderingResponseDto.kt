package no.nav.helse.bakrommet.api.dto.vilkaar

import no.nav.helse.bakrommet.api.dto.interfaces.ApiResponse

data class OppdaterVilkaarsvurderingResponseDto(
    val vilkaarsvurderingDto: VilkaarsvurderingDto,
    val invalidations: List<String>,
) : ApiResponse
