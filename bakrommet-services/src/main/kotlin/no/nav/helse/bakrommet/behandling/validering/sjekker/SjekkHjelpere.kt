package no.nav.helse.bakrommet.behandling.validering.sjekker

import no.nav.helse.bakrommet.behandling.validering.ValideringData
import no.nav.helse.bakrommet.behandling.vilkaar.Vurdering
import no.nav.helse.bakrommet.behandling.vilkaar.VurdertVilkår
import no.nav.helse.bakrommet.kodeverk.Vilkårskode
import no.nav.helse.bakrommet.kodeverk.VilkårskodeBegrunnelse
import no.nav.helse.bakrommet.økonomi.tilInntekt

fun List<VurdertVilkår>.resultat(vilkårskode: Vilkårskode): Vurdering? = this.find { it.vurdering.vilkårskode == vilkårskode.name }?.vurdering?.vurdering

fun <T> List<T>.containsAny(vararg string: T): Boolean = this.any { string.contains(it) }

fun ValideringData.alleSvar(): List<String> = vurderteVilkår.flatMap { it.vurdering.underspørsmål }.map { it.svar }

fun ValideringData.harSvartBegrunnelse(begrunnelse: VilkårskodeBegrunnelse): Boolean = alleSvar().contains(begrunnelse.name)

fun ValideringData.sykepengegrunnlagErUnderHalvG(): Boolean = sykepengegrunnlagErUnderXG(0.5) == true

fun ValideringData.sykepengegrunnlagErUnderEnG(): Boolean = sykepengegrunnlagErUnderXG(1.0) == true

fun ValideringData.sykepengegrunnlagErOverHalvG(): Boolean = sykepengegrunnlagErUnderXG(0.5) == false

fun ValideringData.sykepengegrunnlagErOverEnG(): Boolean = sykepengegrunnlagErUnderXG(1.0) == false

private fun ValideringData.sykepengegrunnlagErUnderXG(x: Double): Boolean? {
    val grunnlag = sykepengegrunnlag?.sykepengegrunnlag?.sykepengegrunnlag?.tilInntekt()
    grunnlag?.let {
        val grunnbeløp = sykepengegrunnlag.sykepengegrunnlag.grunnbeløp
        val xG = grunnbeløp.tilInntekt().times(x)
        if ((grunnlag < xG)) {
            return true
        }
        return false
    }
    return null
}
