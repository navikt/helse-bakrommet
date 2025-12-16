@file:Suppress("ktlint:standard:filename", "ktlint:standard:class-naming")

package no.nav.helse.bakrommet.behandling.validering.sjekker

import no.nav.helse.bakrommet.behandling.validering.ValideringData
import no.nav.helse.bakrommet.behandling.validering.ValideringSjekk
import no.nav.helse.bakrommet.kodeverk.Vilkårskode.OPPTJENING
import no.nav.helse.bakrommet.kodeverk.Vilkårskode.SYK_INAKTIV

object IkkeOppfylt8_2IkkeVurdert8_47 : ValideringSjekk {
    override val id = "IKKE_OPPFYLT_8_2_IKKE_VURDERT_8_47"
    override val tekst = "8-2 vurdert til ikke oppfylt, men 8-47 er ikke vurdert"
    override val sluttvalidering: Boolean = false

    override fun harInkonsistens(data: ValideringData): Boolean {
        data.apply {
            return harVurdertTilIkkeOppfyllt(OPPTJENING) && harIkkeVurdert(SYK_INAKTIV)
        }
    }
}
