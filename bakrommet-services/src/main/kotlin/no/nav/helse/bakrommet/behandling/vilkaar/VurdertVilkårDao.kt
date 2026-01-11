package no.nav.helse.bakrommet.behandling.vilkaar

import no.nav.helse.bakrommet.behandling.BehandlingDbRecord
import java.util.UUID

data class VurdertVilkår(
    val kode: String,
    val vurdering: Vilkaarsvurdering,
)

interface VurdertVilkårDao {
    fun hentVilkårsvurderinger(behandlingId: UUID): List<VurdertVilkår>

    fun hentVilkårsvurdering(
        behandlingId: UUID,
        kode: String,
    ): VurdertVilkår?

    fun slettVilkårsvurdering(
        behandlingId: UUID,
        kode: String,
    ): Int

    fun eksisterer(
        behandlingDbRecord: BehandlingDbRecord,
        kode: Kode,
    ): Boolean

    fun oppdater(
        behandlingDbRecord: BehandlingDbRecord,
        kode: Kode,
        oppdatertVurdering: Vilkaarsvurdering,
    ): Int

    fun leggTil(
        behandlingDbRecord: BehandlingDbRecord,
        kode: Kode,
        vurdering: Vilkaarsvurdering,
    ): Int
}
