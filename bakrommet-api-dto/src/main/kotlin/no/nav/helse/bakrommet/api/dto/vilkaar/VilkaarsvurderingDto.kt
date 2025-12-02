package no.nav.helse.bakrommet.api.dto.vilkaar

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.helse.bakrommet.api.dto.interfaces.ApiResponse

/**
 * Response body for vilkårsvurdering
 * Inkluderer hovedspørsmål som kommer fra kode
 */
data class VilkaarsvurderingDto(
    val hovedspørsmål: String,
    val vurdering: VurderingDto,
    val underspørsmål: List<VilkaarsvurderingUnderspørsmålDto>,
    @param:JsonInclude(JsonInclude.Include.NON_NULL)
    val notat: String? = null,
) : ApiResponse
