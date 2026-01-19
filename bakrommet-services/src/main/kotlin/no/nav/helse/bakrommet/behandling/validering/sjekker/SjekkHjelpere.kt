package no.nav.helse.bakrommet.behandling.validering.sjekker

import no.nav.helse.bakrommet.behandling.validering.ValideringData
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.VurdertVilkår
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.YrkesaktivitetKategorisering
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.YrkesaktivitetKategorisering.Arbeidstaker
import no.nav.helse.bakrommet.kodeverk.Vilkårskode
import no.nav.helse.bakrommet.kodeverk.VilkårskodeBegrunnelse
import no.nav.helse.bakrommet.økonomi.tilInntekt

fun List<VurdertVilkår>.resultat(vilkårskode: Vilkårskode): VurdertVilkår.Utfall? = this.find { it.id.vilkårskode.value == vilkårskode.name }?.vurdering?.utfall

fun ValideringData.harOppfyllt(vilkårskode: Vilkårskode): Boolean = vurderteVilkår.resultat(vilkårskode) == VurdertVilkår.Utfall.OPPFYLT

fun ValideringData.harVurdertTilIkkeOppfyllt(vilkårskode: Vilkårskode): Boolean = vurderteVilkår.resultat(vilkårskode) == VurdertVilkår.Utfall.IKKE_OPPFYLT

fun ValideringData.harVurdert(vilkårskode: Vilkårskode): Boolean =
    vurderteVilkår.resultat(vilkårskode) == VurdertVilkår.Utfall.IKKE_OPPFYLT ||
        vurderteVilkår.resultat(vilkårskode) == VurdertVilkår.Utfall.OPPFYLT

fun ValideringData.harIkkeVurdert(vilkårskode: Vilkårskode): Boolean = !harVurdert(vilkårskode)

fun ValideringData.alleSvar(): List<String> = vurderteVilkår.flatMap { it.vurdering.underspørsmål }.map { it.svar }

fun ValideringData.harSvartBegrunnelse(begrunnelse: VilkårskodeBegrunnelse): Boolean = alleSvar().contains(begrunnelse.name)

fun ValideringData.sykepengegrunnlagErUnderHalvG(): Boolean = sykepengegrunnlagErUnderXG(0.5) == true

fun ValideringData.sykepengegrunnlagErOverHalvG(): Boolean = sykepengegrunnlagErUnderXG(0.5) == false

private fun ValideringData.sykepengegrunnlagErUnderXG(x: Double): Boolean? {
    val grunnlag = sykepengegrunnlag?.sykepengegrunnlag?.sykepengegrunnlag?.tilInntekt()
    grunnlag?.let {
        val grunnbeløp = sykepengegrunnlag.sykepengegrunnlag.grunnbeløp
        val xG = grunnbeløp.tilInntekt().times(x)
        return (grunnlag < xG)
    }
    return null
}

fun ValideringData.harUtbetaling(): Boolean =
    this.beregningData
        ?.spilleromOppdrag
        ?.oppdrag
        ?.any { it.totalbeløp > 0 } ?: false

fun ValideringData.erArbeidstakerFrilanserSnEllerArbeidsledig(): Boolean =
    this.yrkesaktiviteter.filter { it.kategorisering.sykmeldt }.any {
        when (it.kategorisering) {
            is YrkesaktivitetKategorisering.Arbeidsledig -> true
            is Arbeidstaker -> true
            is YrkesaktivitetKategorisering.Frilanser -> true
            is YrkesaktivitetKategorisering.Inaktiv -> false
            is YrkesaktivitetKategorisering.SelvstendigNæringsdrivende -> true
        }
    }
