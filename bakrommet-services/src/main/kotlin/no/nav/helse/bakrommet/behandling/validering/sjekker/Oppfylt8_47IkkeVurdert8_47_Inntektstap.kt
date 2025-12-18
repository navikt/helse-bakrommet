@file:Suppress("ktlint:standard:filename", "ktlint:standard:class-naming")

package no.nav.helse.bakrommet.behandling.validering.sjekker

import no.nav.helse.bakrommet.behandling.validering.ValideringData
import no.nav.helse.bakrommet.behandling.validering.ValideringSjekk
import no.nav.helse.bakrommet.kodeverk.Vilkårskode.INAKTIV_INNTEKTSTAP_OG_MINSTE_SYKEPENGEGRUNNLAG
import no.nav.helse.bakrommet.kodeverk.Vilkårskode.SYK_INAKTIV

object Oppfylt8_47IkkeVurdert8_47_Inntektstap : ValideringSjekk {
    override val id = "OPPFYLT_8_47_IKKE_VURDERT_8_47_INNTEKTSTAP"
    override val tekst = "8-47 inntektstap er ikke vurdert"
    override val sluttvalidering: Boolean = false

    override fun harInkonsistens(data: ValideringData): Boolean {
        data.apply {
            return harOppfyllt(SYK_INAKTIV) && harIkkeVurdert(INAKTIV_INNTEKTSTAP_OG_MINSTE_SYKEPENGEGRUNNLAG)
        }
    }
}
