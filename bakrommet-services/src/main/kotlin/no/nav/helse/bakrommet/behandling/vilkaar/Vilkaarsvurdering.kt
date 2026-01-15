package no.nav.helse.bakrommet.behandling.vilkaar

import com.fasterxml.jackson.annotation.JsonInclude

enum class Vurdering {
    OPPFYLT,
    IKKE_OPPFYLT,
    IKKE_RELEVANT,
    SKAL_IKKE_VURDERES,
}

data class VilkaarsvurderingUnderspørsmål(
    val spørsmål: String,
    val svar: String,
)

/**
 * Response body for vilkårsvurdering
 * Inkluderer hovedspørsmål som kommer fra kode
 */
data class Vilkaarsvurdering(
    val vilkårskode: String,
    val hovedspørsmål: String,
    val vurdering: Vurdering,
    val underspørsmål: List<VilkaarsvurderingUnderspørsmål>,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val notat: String? = null,
)
