package no.nav.helse.bakrommet.behandling.validering.sjekker

import no.nav.helse.bakrommet.behandling.validering.ValideringData
import no.nav.helse.bakrommet.behandling.validering.ValideringSjekk
import kotlin.collections.contains

object AvslåttBegrunnelseUtenVilkårsvurdering : ValideringSjekk {
    override val id = "AVSLÅTT_BEGRUNNELSE_UTEN_VILKÅRSVURDERING"
    override val tekst = "Behandlingen har avslåtte dager med begrunnelser uten tilhørende vilkårsvurdering"
    override val sluttvalidering: Boolean = false

    override fun harInkonsistens(data: ValideringData): Boolean {
        val avslåttBegrunnelser =
            data.yrkesaktiviteter
                .flatMap { it.dagoversikt ?: emptyList() }
                .flatMap { it.avslåttBegrunnelse ?: emptyList() }
                .toSet()
        val vilkårskoder =
            data.vurderteVilkår
                .flatMap { it.vurdering.underspørsmål }
                .map { it.svar }
                .toSet()
        return avslåttBegrunnelser.subtract(vilkårskoder).isNotEmpty()
    }
}
