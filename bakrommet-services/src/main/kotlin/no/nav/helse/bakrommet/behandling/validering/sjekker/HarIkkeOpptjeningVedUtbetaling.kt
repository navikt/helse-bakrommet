package no.nav.helse.bakrommet.behandling.validering.sjekker

import no.nav.helse.bakrommet.behandling.validering.ValideringData
import no.nav.helse.bakrommet.behandling.validering.ValideringSjekk
import no.nav.helse.bakrommet.kodeverk.Vilkårskode.OPPTJENING

object HarIkkeOpptjeningVedUtbetaling : ValideringSjekk {
    override val id = "IKKE_GODKJENT_OPPTJENING_VED_UTBETALING"
    override val tekst = "Det utbetales sykepenger selv om opptjeningsvilkåret ikke er oppfylt"
    override val sluttvalidering: Boolean = true

    override fun harInkonsistens(data: ValideringData): Boolean {
        data.apply {
            return erArbeidstakerFrilanserSnEllerArbeidsledig() && harUtbetaling() && !harOppfyllt(OPPTJENING)
        }
    }
}
