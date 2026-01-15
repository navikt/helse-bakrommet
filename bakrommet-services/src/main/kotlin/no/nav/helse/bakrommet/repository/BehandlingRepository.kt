package no.nav.helse.bakrommet.repository

import no.nav.helse.bakrommet.domain.saksbehandling.behandling.Behandling
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingId

interface BehandlingRepository {
    fun finn(behandlingId: BehandlingId): Behandling?

    fun hent(behandlingId: BehandlingId): Behandling = finn(behandlingId) ?: error("Fant ingen behandling med id=${behandlingId.value}")

    fun lagre(behandling: Behandling)
}
