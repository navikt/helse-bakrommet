package no.nav.helse.bakrommet.api.yrkesaktivitet

import io.ktor.server.routing.*
import no.nav.helse.bakrommet.api.behandling.hentOgVerifiserBehandling
import no.nav.helse.bakrommet.api.yrkesaktivitetId
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.Yrkesaktivitetsperiode
import no.nav.helse.bakrommet.infrastruktur.db.AlleDaoer
import kotlin.collections.firstOrNull

internal fun AlleDaoer.hentOgVerifiserYrkesaktivitet(call: RoutingCall): Yrkesaktivitetsperiode {
    val behandling = hentOgVerifiserBehandling(call)
    val yrkesaktivitetId = call.yrkesaktivitetId()
    val yrkesaktivitet =
        yrkesaktivitetsperiodeRepository
            .finn(behandling.id)
            .firstOrNull { it.id == yrkesaktivitetId }
            ?: throw IllegalArgumentException("Fant ikke yrkesaktivitet med id=${yrkesaktivitetId.value} for behandlingId=${behandling.id.value}")

    return yrkesaktivitet
}
