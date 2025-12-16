@file:Suppress("ktlint:standard:filename", "ktlint:standard:class-naming")

package no.nav.helse.bakrommet.behandling.validering.sjekker

import no.nav.helse.bakrommet.behandling.validering.ValideringData
import no.nav.helse.bakrommet.behandling.validering.ValideringSjekk
import no.nav.helse.bakrommet.behandling.vilkaar.Vurdering.IKKE_OPPFYLT
import no.nav.helse.bakrommet.behandling.vilkaar.Vurdering.OPPFYLT
import no.nav.helse.bakrommet.kodeverk.Vilkårskode.OPPTJENING
import no.nav.helse.bakrommet.kodeverk.Vilkårskode.SYK_INAKTIV

object IkkeOppfylt8_2IkkeVurdert8_47 : ValideringSjekk {
    override val id = "ikke8-2ikke8-47"
    override val tekst = "8-2 vurdert til ikke oppfylt, men 8-47 er ikke vurdert"

    override fun harInkonsistens(data: ValideringData): Boolean {
        val opptjeningIkkeOppfylt = data.vurderteVilkår.resultat(OPPTJENING) == IKKE_OPPFYLT
        val inaktivIkkeVurdert = data.vurderteVilkår.resultat(SYK_INAKTIV) !in listOf(OPPFYLT, IKKE_OPPFYLT)
        return opptjeningIkkeOppfylt && inaktivIkkeVurdert
    }
}
