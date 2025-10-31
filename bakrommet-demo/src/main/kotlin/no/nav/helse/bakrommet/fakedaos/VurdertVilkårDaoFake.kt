package no.nav.helse.bakrommet.fakedaos

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.bakrommet.saksbehandlingsperiode.Saksbehandlingsperiode
import no.nav.helse.bakrommet.saksbehandlingsperiode.vilkaar.Kode
import no.nav.helse.bakrommet.saksbehandlingsperiode.vilkaar.VurdertVilkår
import no.nav.helse.bakrommet.saksbehandlingsperiode.vilkaar.VurdertVilkårDao
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class VurdertVilkårDaoFake : VurdertVilkårDao {
    private val vurderinger = ConcurrentHashMap<Pair<UUID, String>, VurdertVilkår>()

    override fun hentVilkårsvurderinger(saksbehandlingsperiodeId: UUID): List<VurdertVilkår> = vurderinger.filterKeys { it.first == saksbehandlingsperiodeId }.values.toList()

    override fun hentVilkårsvurdering(
        saksbehandlingsperiodeId: UUID,
        kode: String,
    ): VurdertVilkår? = vurderinger[saksbehandlingsperiodeId to kode]

    override fun slettVilkårsvurdering(
        saksbehandlingsperiodeId: UUID,
        kode: String,
    ): Int = if (vurderinger.remove(saksbehandlingsperiodeId to kode) != null) 1 else 0

    override fun eksisterer(
        behandling: Saksbehandlingsperiode,
        kode: Kode,
    ): Boolean = vurderinger.containsKey(behandling.id to kode.kode)

    override fun oppdater(
        behandling: Saksbehandlingsperiode,
        kode: Kode,
        oppdatertVurdering: JsonNode,
    ): Int {
        val key = behandling.id to kode.kode
        val eksisterende = vurderinger[key] ?: return 0
        vurderinger[key] = eksisterende.copy(vurdering = oppdatertVurdering)
        return 1
    }

    override fun leggTil(
        behandling: Saksbehandlingsperiode,
        kode: Kode,
        vurdering: JsonNode,
    ): Int {
        vurderinger[behandling.id to kode.kode] = VurdertVilkår(kode = kode.kode, vurdering = vurdering)
        return 1
    }
}
