package no.nav.helse.bakrommet.behandling.validering.sjekker

import no.nav.helse.bakrommet.behandling.validering.ValideringData
import no.nav.helse.bakrommet.behandling.validering.ValideringSjekk
import no.nav.helse.bakrommet.behandling.vilkaar.Vurdering
import no.nav.helse.bakrommet.behandling.vilkaar.Vurdering.IKKE_OPPFYLT
import no.nav.helse.bakrommet.behandling.vilkaar.Vurdering.OPPFYLT
import no.nav.helse.bakrommet.behandling.vilkaar.VurdertVilkår
import no.nav.helse.bakrommet.kodeverk.Vilkårskode
import no.nav.helse.bakrommet.kodeverk.Vilkårskode.INAKTIV
import no.nav.helse.bakrommet.kodeverk.Vilkårskode.OPPTJENING

fun List<VurdertVilkår>.resultat(vilkårskode: Vilkårskode): Vurdering? = this.find { it.vurdering.vilkårskode == vilkårskode.name }?.vurdering?.vurdering

object IkkeOppfylt8_2IkkeVurdert8_47 : ValideringSjekk {
    override val id = "ikke8-2ikke8-47"
    override val tekst = "8-2 vurdert til ikke oppfylt, men 8-47 er ikke vurdert"

    override fun sjekkOmOk(data: ValideringData): Boolean {
        data.vurderteVilkår
            .let { v ->
                val opptjeningIkkeOppfylt = v.resultat(OPPTJENING) == IKKE_OPPFYLT
                val inaktivIkkeVurdert = v.resultat(INAKTIV) !in listOf(OPPFYLT, IKKE_OPPFYLT)
                opptjeningIkkeOppfylt && inaktivIkkeVurdert
            }.let { inkonsistens ->
                return !inkonsistens
            }
    }
}
