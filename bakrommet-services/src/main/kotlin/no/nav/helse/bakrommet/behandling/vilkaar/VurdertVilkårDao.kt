package no.nav.helse.bakrommet.behandling.vilkaar

import no.nav.helse.bakrommet.behandling.BehandlingDbRecord
import java.util.UUID

data class LegacyVurdertVilkår(
    val kode: String,
    val vurdering: Vilkaarsvurdering,
)

interface VurdertVilkårDao {
    fun hentVilkårsvurderinger(behandlingId: UUID): List<LegacyVurdertVilkår>

    fun leggTil(
        behandlingDbRecord: BehandlingDbRecord,
        kode: Kode,
        vurdering: Vilkaarsvurdering,
    ): Int
}
