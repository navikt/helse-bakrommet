package no.nav.helse.bakrommet.behandling.validering.sjekker

import no.nav.helse.bakrommet.behandling.validering.ValideringData
import no.nav.helse.bakrommet.behandling.validering.ValideringSjekk
import no.nav.helse.bakrommet.kodeverk.VilkårskodeBegrunnelse.MINSTEINNTEKT

object TjenerUnderEnHalvGMenVurdertTilOver : ValideringSjekk {
    override val id = "SPG_UNDER_HALV_G_VURDERT_TIL_OVER"
    override val tekst = "Sykepengegrunnlaget er under en halv G, men vilkåret er vurdert til over en halv G"
    override val sluttvalidering: Boolean = false

    override fun harInkonsistens(data: ValideringData): Boolean = data.sykepengegrunnlagErUnderHalvG() && data.harSvartBegrunnelse(MINSTEINNTEKT)
}
