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
        require(value.erGyldigSomKode()) { "Ugyldig format på Kode" }
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
    vurdering: Vurdering,
) {
    var vurdering: Vurdering = vurdering
        private set

    fun kopierTil(nyBehandlingId: BehandlingId): VurdertVilkår =
        VurdertVilkår(
            id =
                VilkårsvurderingId(
                    behandlingId = nyBehandlingId,
                    vilkårskode = id.vilkårskode,
                ),
            vurdering = vurdering.copy(),
        )

    fun nyVurdering(
        vurdering: Vurdering,
    ) {
        this.vurdering = vurdering
    }

    companion object {
        fun ny(
            vilkårsvurderingId: VilkårsvurderingId,
            vurdering: Vurdering,
        ) = VurdertVilkår(
            id = vilkårsvurderingId,
            vurdering = vurdering,
        )
    }

    enum class Utfall {
        OPPFYLT,
        IKKE_OPPFYLT,
        IKKE_RELEVANT,
        SKAL_IKKE_VURDERES,
    }

    data class Vurdering(
        val underspørsmål: List<VilkårsvurderingUnderspørsmål>,
        val notat: String?,
        val utfall: Utfall,
    )
}
