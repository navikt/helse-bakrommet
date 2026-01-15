package no.nav.helse.bakrommet.fakedaos

import no.nav.helse.bakrommet.behandling.BehandlingDbRecord
import no.nav.helse.bakrommet.behandling.vilkaar.Kode
import no.nav.helse.bakrommet.behandling.vilkaar.LegacyVurdertVilkår
import no.nav.helse.bakrommet.behandling.vilkaar.Vilkaarsvurdering
import no.nav.helse.bakrommet.behandling.vilkaar.VurdertVilkårDao
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingId
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.Vilkårskode
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.VilkårsvurderingId
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.VurdertVilkår
import no.nav.helse.bakrommet.repository.VilkårsvurderingRepository
import java.util.UUID
import no.nav.helse.bakrommet.behandling.vilkaar.VilkaarsvurderingUnderspørsmål as LegacyUnderspørsmål
import no.nav.helse.bakrommet.behandling.vilkaar.Vurdering as LegacyVurdering
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.VilkårsvurderingUnderspørsmål as DomainUnderspørsmål

class VurdertVilkårDaoFake(
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
) : VurdertVilkårDao {
    override fun hentVilkårsvurderinger(behandlingId: UUID): List<LegacyVurdertVilkår> = vilkårsvurderingRepository.hentAlle(BehandlingId(behandlingId)).map { it.tilLegacy() }

    override fun leggTil(
        behandlingDbRecord: BehandlingDbRecord,
        kode: Kode,
        vurdering: Vilkaarsvurdering,
    ): Int {
        val vilkårsvurderingId =
            VilkårsvurderingId(
                behandlingId = BehandlingId(behandlingDbRecord.id),
                vilkårskode = Vilkårskode(kode.kode),
            )

        val domainVurdertVilkår =
            VurdertVilkår.ny(
                vilkårsvurderingId = vilkårsvurderingId,
                vurdering = vurdering.tilDomainVurdering(),
            )

        vilkårsvurderingRepository.lagre(domainVurdertVilkår)
        return 1
    }

    private fun VurdertVilkår.tilLegacy(): LegacyVurdertVilkår {
        val kode = id.vilkårskode.value
        return LegacyVurdertVilkår(
            kode = kode,
            vurdering =
                Vilkaarsvurdering(
                    vilkårskode = kode,
                    hovedspørsmål = kode,
                    vurdering = vurdering.utfall.tilLegacyVurdering(),
                    underspørsmål = vurdering.underspørsmål.map { it.tilLegacy() },
                    notat = vurdering.notat,
                ),
        )
    }

    private fun Vilkaarsvurdering.tilDomainVurdering(): VurdertVilkår.Vurdering =
        VurdertVilkår.Vurdering(
            underspørsmål = underspørsmål.map { it.tilDomain() },
            notat = notat,
            utfall = vurdering.tilDomainUtfall(),
        )

    private fun LegacyUnderspørsmål.tilDomain(): DomainUnderspørsmål = DomainUnderspørsmål(spørsmål = spørsmål, svar = svar)

    private fun DomainUnderspørsmål.tilLegacy(): LegacyUnderspørsmål = LegacyUnderspørsmål(spørsmål = spørsmål, svar = svar)

    private fun LegacyVurdering.tilDomainUtfall(): VurdertVilkår.Utfall = VurdertVilkår.Utfall.valueOf(name)

    private fun VurdertVilkår.Utfall.tilLegacyVurdering(): LegacyVurdering = LegacyVurdering.valueOf(name)
}
