@file:Suppress("ktlint:standard:filename", "ktlint:standard:class-naming")

package no.nav.helse.bakrommet.behandling.validering.sjekker

import no.nav.helse.bakrommet.behandling.validering.ValideringData
import no.nav.helse.bakrommet.behandling.validering.ValideringSjekk
import no.nav.helse.bakrommet.behandling.vilkaar.Vurdering.OPPFYLT
import no.nav.helse.bakrommet.kodeverk.Vilkårskode.SYK_INAKTIV
import no.nav.helse.bakrommet.kodeverk.VilkårskodeBegrunnelser.*
import no.nav.helse.bakrommet.kodeverk.VilkårskodeBegrunnelser.UTE_AV_ARBEID_IKKE_INNTEKTSTAP
import no.nav.helse.bakrommet.kodeverk.VilkårskodeBegrunnelser.UTE_AV_ARBEID_INNEKTSTAP

object Oppfylt8_47IkkeVurdert8_47_Inntektstap : ValideringSjekk {
    override val id = "OPPFYLT_8_47_IKKE_VURDERT_8_47_INNTEKTSTAP"
    override val tekst = "8-47 inntektstap er ikke vurdert"

    override fun harInkonsistens(data: ValideringData): Boolean {
        val inaktivErOppfyllt = data.vurderteVilkår.resultat(SYK_INAKTIV) == OPPFYLT
        val alleSvar = data.vurderteVilkår.flatMap { it.vurdering.underspørsmål }.map { it.svar }

        val harSvartInntektstap =
            alleSvar.containsAny(
                UTE_AV_ARBEID_INNEKTSTAP,
                UTE_AV_ARBEID_IKKE_INNTEKTSTAP,
            )

        val harSvartMinst1G = alleSvar.containsAny(UTE_AV_ARBEID_MINST_1G, UTE_AV_ARBEID_MINDRE_ENN_1G)
        val harSvartAltOmInntektstap = harSvartInntektstap && harSvartMinst1G

        return inaktivErOppfyllt && !harSvartAltOmInntektstap
    }
}
