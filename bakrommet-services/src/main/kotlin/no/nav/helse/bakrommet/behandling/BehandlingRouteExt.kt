package no.nav.helse.bakrommet.behandling

import io.ktor.server.routing.RoutingCall
import no.nav.helse.bakrommet.periodeUUID
import no.nav.helse.bakrommet.personId

fun RoutingCall.periodeReferanse() =
    SaksbehandlingsperiodeReferanse(
        spilleromPersonId = personId(),
        periodeUUID = periodeUUID(),
    )
