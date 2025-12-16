package no.nav.helse.bakrommet.behandling.validering.sjekker

import no.nav.helse.bakrommet.behandling.vilkaar.Vurdering
import no.nav.helse.bakrommet.behandling.vilkaar.VurdertVilkår
import no.nav.helse.bakrommet.kodeverk.Vilkårskode

fun List<VurdertVilkår>.resultat(vilkårskode: Vilkårskode): Vurdering? = this.find { it.vurdering.vilkårskode == vilkårskode.name }?.vurdering?.vurdering

fun <T> List<T>.containsAny(vararg string: T): Boolean = this.any { string.contains(it) }
