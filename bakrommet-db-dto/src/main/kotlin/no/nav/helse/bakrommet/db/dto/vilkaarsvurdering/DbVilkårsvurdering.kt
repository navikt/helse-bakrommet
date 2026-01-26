package no.nav.helse.bakrommet.db.dto.vilkaarsvurdering

import com.fasterxml.jackson.annotation.JsonInclude

enum class Vurdering {
    OPPFYLT,
    IKKE_OPPFYLT,
    IKKE_RELEVANT,
    SKAL_IKKE_VURDERES,
}

data class DbVilkårsvurderingUnderspørsmål(
    val spørsmål: String,
    val svar: String,
)

/**
 * Databasekontrakt
 * Inkluderer hovedspørsmål som kommer fra kode
 */
data class DbVilkårsvurdering(
    val vilkårskode: String,
    val hovedspørsmål: String,
    val vurdering: Vurdering,
    val underspørsmål: List<DbVilkårsvurderingUnderspørsmål>,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val notat: String? = null,
)
