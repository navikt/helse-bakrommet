package no.nav.helse.bakrommet.fakedaos

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.runBlocking
import no.nav.helse.bakrommet.hentSessionid
import no.nav.helse.bakrommet.saksbehandlingsperiode.Saksbehandlingsperiode
import no.nav.helse.bakrommet.saksbehandlingsperiode.vilkaar.Kode
import no.nav.helse.bakrommet.saksbehandlingsperiode.vilkaar.VurdertVilkår
import no.nav.helse.bakrommet.saksbehandlingsperiode.vilkaar.VurdertVilkårDao
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class VurdertVilkårDaoFake : VurdertVilkårDao {
    // Map av sessionId -> (periodeId, kode) -> VurdertVilkår
    private val sessionData = ConcurrentHashMap<String, ConcurrentHashMap<Pair<UUID, String>, VurdertVilkår>>()

    private fun getSessionMap(): ConcurrentHashMap<Pair<UUID, String>, VurdertVilkår> =
        runBlocking {
            val sessionId = hentSessionid()

            sessionData.getOrPut(sessionId) { ConcurrentHashMap() }
        }

    private val vurderinger: ConcurrentHashMap<Pair<UUID, String>, VurdertVilkår>
        get() = getSessionMap()

    override suspend fun hentVilkårsvurderinger(saksbehandlingsperiodeId: UUID): List<VurdertVilkår> = vurderinger.filterKeys { it.first == saksbehandlingsperiodeId }.values.toList()

    override suspend fun hentVilkårsvurdering(
        saksbehandlingsperiodeId: UUID,
        kode: String,
    ): VurdertVilkår? = vurderinger[saksbehandlingsperiodeId to kode]

    override suspend fun slettVilkårsvurdering(
        saksbehandlingsperiodeId: UUID,
        kode: String,
    ): Int = if (vurderinger.remove(saksbehandlingsperiodeId to kode) != null) 1 else 0

    override suspend fun eksisterer(
        behandling: Saksbehandlingsperiode,
        kode: Kode,
    ): Boolean = vurderinger.containsKey(behandling.id to kode.kode)

    override suspend fun oppdater(
        behandling: Saksbehandlingsperiode,
        kode: Kode,
        oppdatertVurdering: JsonNode,
    ): Int {
        val key = behandling.id to kode.kode
        val eksisterende = vurderinger[key] ?: return 0
        vurderinger[key] = eksisterende.copy(vurdering = oppdatertVurdering)
        return 1
    }

    override suspend fun leggTil(
        behandling: Saksbehandlingsperiode,
        kode: Kode,
        vurdering: JsonNode,
    ): Int {
        vurderinger[behandling.id to kode.kode] = VurdertVilkår(kode = kode.kode, vurdering = vurdering)
        return 1
    }
}
