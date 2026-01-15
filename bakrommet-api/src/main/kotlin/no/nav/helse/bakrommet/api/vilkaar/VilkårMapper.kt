package no.nav.helse.bakrommet.api.vilkaar

import no.nav.helse.bakrommet.api.dto.vilkaar.VilkaarsvurderingDto
import no.nav.helse.bakrommet.api.dto.vilkaar.VilkaarsvurderingUnderspørsmålDto
import no.nav.helse.bakrommet.api.dto.vilkaar.VurderingDto
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.VurdertVilkår

internal fun VurdertVilkår.skapVilkaarsvurderingDto(): VilkaarsvurderingDto =
    VilkaarsvurderingDto(
        hovedspørsmål = id.vilkårskode.value,
        vilkårskode = id.vilkårskode.value,
        vurdering = vurdering.utfall.tilVurderingDto(),
        underspørsmål = vurdering.underspørsmål.map { it.tilVilkaarsvurderingUnderspørsmålDto() },
        notat = vurdering.notat,
    )

private fun VurdertVilkår.Utfall.tilVurderingDto(): VurderingDto =
    when (this) {
        VurdertVilkår.Utfall.OPPFYLT -> VurderingDto.OPPFYLT
        VurdertVilkår.Utfall.IKKE_OPPFYLT -> VurderingDto.IKKE_OPPFYLT
        VurdertVilkår.Utfall.IKKE_RELEVANT -> VurderingDto.IKKE_RELEVANT
        VurdertVilkår.Utfall.SKAL_IKKE_VURDERES -> VurderingDto.SKAL_IKKE_VURDERES
    }

private fun no.nav.helse.bakrommet.domain.saksbehandling.behandling.VilkårsvurderingUnderspørsmål.tilVilkaarsvurderingUnderspørsmålDto(): VilkaarsvurderingUnderspørsmålDto =
    VilkaarsvurderingUnderspørsmålDto(
        spørsmål = spørsmål,
        svar = svar,
    )
