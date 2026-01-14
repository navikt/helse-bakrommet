package no.nav.helse.bakrommet.domain.saksbehandling.behandling

import java.util.UUID

data class VilkårsvurderingUnderspørsmål(
    val spørsmål: String,
    val svar: String,
)

@JvmInline
value class Vilkårskode(
    val value: String,
) {
    init {
        check(value.erGyldigSomKode()) { "Vilkårskode har feil format" }
    }

    private companion object {
        private fun String.erGyldigSomKode(): Boolean =
            runCatching {
                // Først sjekk om det er en gyldig UUID
                UUID.fromString(this)
            }.fold(
                onSuccess = { true },
                onFailure = {
                    // Hvis ikke UUID, bruk opprinnelig regex
                    val regex = "^[A-ZÆØÅ0-9_]*$".toRegex()
                    regex.matches(this)
                },
            )
    }
}

data class VilkårsvurderingId(
    val behandlingId: BehandlingId,
    val vilkårskode: Vilkårskode,
)

class VurdertVilkår(
    val id: VilkårsvurderingId,
    val hovedspørsmål: String,
    val underspørsmål: List<VilkårsvurderingUnderspørsmål>,
    vurdering: Vurdering,
    notat: String?,
) {
    var vurdering: Vurdering = vurdering
        private set
    var notat: String? = notat
        private set

    fun nyVurdering(
        vurdering: Vurdering,
        notat: String?,
    ) {
        this.vurdering = vurdering
        this.notat = notat
    }

    enum class Vurdering {
        OPPFYLT,
        IKKE_OPPFYLT,
        IKKE_RELEVANT,
        SKAL_IKKE_VURDERES,
    }
}
