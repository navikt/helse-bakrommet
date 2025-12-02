package no.nav.helse.bakrommet.api.vilkaar

import no.nav.helse.bakrommet.api.dto.vilkaar.VilkaarsvurderingDto
import no.nav.helse.bakrommet.api.dto.vilkaar.VilkaarsvurderingRequestDto
import no.nav.helse.bakrommet.api.dto.vilkaar.VilkaarsvurderingUnderspørsmålDto
import no.nav.helse.bakrommet.api.dto.vilkaar.VurderingDto
import no.nav.helse.bakrommet.behandling.vilkaar.Vilkaarsvurdering
import no.nav.helse.bakrommet.behandling.vilkaar.VilkaarsvurderingRequest
import no.nav.helse.bakrommet.behandling.vilkaar.VilkaarsvurderingUnderspørsmål
import no.nav.helse.bakrommet.behandling.vilkaar.Vurdering
import no.nav.helse.bakrommet.behandling.vilkaar.VurdertVilkår

fun VurdertVilkår.tilVilkaarsvurderingDto(): VilkaarsvurderingDto = vurdering.tilVilkaarsvurderingDto()

private fun Vilkaarsvurdering.tilVilkaarsvurderingDto(): VilkaarsvurderingDto =
    VilkaarsvurderingDto(
        hovedspørsmål = hovedspørsmål,
        vurdering = vurdering.tilVurderingDto(),
        underspørsmål = underspørsmål.map { it.tilVilkaarsvurderingUnderspørsmålDto() },
        notat = notat,
    )

private fun VilkaarsvurderingUnderspørsmål.tilVilkaarsvurderingUnderspørsmålDto(): VilkaarsvurderingUnderspørsmålDto =
    VilkaarsvurderingUnderspørsmålDto(
        spørsmål = spørsmål,
        svar = svar,
    )

private fun Vurdering.tilVurderingDto(): VurderingDto =
    when (this) {
        Vurdering.OPPFYLT -> VurderingDto.OPPFYLT
        Vurdering.IKKE_OPPFYLT -> VurderingDto.IKKE_OPPFYLT
        Vurdering.IKKE_RELEVANT -> VurderingDto.IKKE_RELEVANT
        Vurdering.SKAL_IKKE_VURDERES -> VurderingDto.SKAL_IKKE_VURDERES
    }

fun VilkaarsvurderingRequestDto.tilVilkaarsvurderingRequest(): VilkaarsvurderingRequest =
    VilkaarsvurderingRequest(
        vurdering = vurdering.tilVurdering(),
        underspørsmål = underspørsmål.map { it.tilVilkaarsvurderingUnderspørsmål() },
        notat = notat,
    )

private fun VilkaarsvurderingUnderspørsmålDto.tilVilkaarsvurderingUnderspørsmål(): VilkaarsvurderingUnderspørsmål =
    VilkaarsvurderingUnderspørsmål(
        spørsmål = spørsmål,
        svar = svar,
    )

private fun VurderingDto.tilVurdering(): Vurdering =
    when (this) {
        VurderingDto.OPPFYLT -> Vurdering.OPPFYLT
        VurderingDto.IKKE_OPPFYLT -> Vurdering.IKKE_OPPFYLT
        VurderingDto.IKKE_RELEVANT -> Vurdering.IKKE_RELEVANT
        VurderingDto.SKAL_IKKE_VURDERES -> Vurdering.SKAL_IKKE_VURDERES
    }
