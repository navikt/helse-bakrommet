package no.nav.helse.bakrommet.api.validering

import no.nav.helse.bakrommet.api.dto.validering.ValideringDto
import no.nav.helse.bakrommet.behandling.validering.SjekkResultat

fun SjekkResultat.tilValideringDto() =
    ValideringDto(
        id = this.id,
        tekst = this.tekst,
    )
