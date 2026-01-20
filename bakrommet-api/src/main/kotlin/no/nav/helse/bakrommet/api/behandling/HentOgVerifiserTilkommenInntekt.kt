package no.nav.helse.bakrommet.api.behandling

import io.ktor.server.routing.RoutingCall
import no.nav.helse.bakrommet.api.tilkommenInntektId
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.TilkommenInntekt
import no.nav.helse.bakrommet.infrastruktur.db.AlleDaoer

internal fun AlleDaoer.hentTilkommenInntekt(call: RoutingCall): TilkommenInntekt {
    val tilkommenInntektId = call.tilkommenInntektId()
    val tilkommenInntekt = tilkommenInntektRepository.finn(tilkommenInntektId) ?: error("TilkommenInntekt med id ${tilkommenInntektId.value} finnes ikke")
    return tilkommenInntekt
}
