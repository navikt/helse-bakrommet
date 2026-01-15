package no.nav.helse.bakrommet.fakedaos

import no.nav.helse.bakrommet.behandling.BehandlingDbRecord
import no.nav.helse.bakrommet.behandling.vilkaar.Kode
import no.nav.helse.bakrommet.behandling.vilkaar.LegacyVurdertVilkår
import no.nav.helse.bakrommet.behandling.vilkaar.Vilkaarsvurdering
import no.nav.helse.bakrommet.behandling.vilkaar.VurdertVilkårDao
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class VurdertVilkårDaoFake : VurdertVilkårDao {
    private val vurderinger = ConcurrentHashMap<Pair<UUID, String>, LegacyVurdertVilkår>()

    override fun hentVilkårsvurderinger(behandlingId: UUID): List<LegacyVurdertVilkår> = vurderinger.filterKeys { it.first == behandlingId }.values.toList()

    override fun hentVilkårsvurdering(
        behandlingId: UUID,
        kode: String,
    ): LegacyVurdertVilkår? = vurderinger[behandlingId to kode]

    override fun slettVilkårsvurdering(
        behandlingId: UUID,
        kode: String,
    ): Int = if (vurderinger.remove(behandlingId to kode) != null) 1 else 0

    override fun eksisterer(
        behandlingDbRecord: BehandlingDbRecord,
        kode: Kode,
    ): Boolean = vurderinger.containsKey(behandlingDbRecord.id to kode.kode)

    override fun oppdater(
        behandlingDbRecord: BehandlingDbRecord,
        kode: Kode,
        oppdatertVurdering: Vilkaarsvurdering,
    ): Int {
        val key = behandlingDbRecord.id to kode.kode
        val eksisterende = vurderinger[key] ?: return 0
        vurderinger[key] = eksisterende.copy(vurdering = oppdatertVurdering)
        return 1
    }

    override fun leggTil(
        behandlingDbRecord: BehandlingDbRecord,
        kode: Kode,
        vurdering: Vilkaarsvurdering,
    ): Int {
        vurderinger[behandlingDbRecord.id to kode.kode] = LegacyVurdertVilkår(kode = kode.kode, vurdering = vurdering)
        return 1
    }
}
