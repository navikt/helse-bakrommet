package no.nav.helse.bakrommet.api.behandling

import io.ktor.server.routing.*
import no.nav.helse.bakrommet.api.behandlingId
import no.nav.helse.bakrommet.api.pseudoId
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.Behandling
import no.nav.helse.bakrommet.infrastruktur.db.AlleDaoer

internal fun AlleDaoer.hentOgVerifiserBehandling(call: RoutingCall): Behandling {
    val behandlingId = call.behandlingId()
    val pseudoId = call.pseudoId()
    val behandling = behandlingRepository.hent(behandlingId)
    val naturligIdent = personPseudoIdDao.hentNaturligIdent(pseudoId)
    if (!behandling.gjelder(naturligIdent)) {
        throw IllegalArgumentException("Behandling ${behandlingId.value} gjelder ikke personen med pseudoId=${pseudoId.value}")
    }
    return behandling
}
