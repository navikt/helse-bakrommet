package no.nav.helse.bakrommet.behandling.validering.sjekker

import no.nav.helse.bakrommet.behandling.validering.ValideringData
import no.nav.helse.bakrommet.behandling.validering.ValideringSjekk
import no.nav.helse.bakrommet.kodeverk.VilkårskodeBegrunnelse.IKKE_MINSTEINNTEKT

object TjenerOverEnHalvGMenVurdertTilUnder : ValideringSjekk {
    override val id = "SPG_OVER_HALV_G_VURDERT_TIL_UNDER"
    override val tekst = "Sykepengegrunnlaget er over en halv G, men vilkåret er vurdert til under en halv G"
    override val sluttvalidering: Boolean = false

    override fun harInkonsistens(data: ValideringData): Boolean = data.sykepengegrunnlagErOverHalvG() && data.harSvartBegrunnelse(IKKE_MINSTEINNTEKT)
}
