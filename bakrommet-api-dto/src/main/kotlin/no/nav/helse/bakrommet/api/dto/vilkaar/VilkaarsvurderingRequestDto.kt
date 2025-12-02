package no.nav.helse.bakrommet.api.dto.vilkaar

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * Request body for å opprette/oppdatere vilkårsvurdering
 * Mangler hovedspørsmål som kommer fra URL-parameter
 */
data class VilkaarsvurderingRequestDto(
    val vurdering: VurderingDto,
    val underspørsmål: List<VilkaarsvurderingUnderspørsmålDto>,
    @param:JsonInclude(JsonInclude.Include.NON_NULL)
    val notat: String? = null,
)
