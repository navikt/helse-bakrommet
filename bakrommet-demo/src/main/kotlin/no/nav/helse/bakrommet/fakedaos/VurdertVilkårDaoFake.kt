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

    override fun leggTil(
        behandlingDbRecord: BehandlingDbRecord,
        kode: Kode,
        vurdering: Vilkaarsvurdering,
    ): Int {
        vurderinger[behandlingDbRecord.id to kode.kode] = LegacyVurdertVilkår(kode = kode.kode, vurdering = vurdering)
        return 1
    }
}
