package no.nav.helse.bakrommet.repository

import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingId
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.VilkårsvurderingId
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.VurdertVilkår

interface VilkårsvurderingRepository {
    fun finn(vilkårsvurderingId: VilkårsvurderingId): VurdertVilkår?

    fun hentAlle(behandlingId: BehandlingId): List<VurdertVilkår>

    fun lagre(vurdertVilkår: VurdertVilkår)

    fun slett(vilkårsvurderingId: VilkårsvurderingId)
}
