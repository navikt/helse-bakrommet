package no.nav.helse.bakrommet.repository

import no.nav.helse.bakrommet.domain.saksbehandling.behandling.Behandling
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingId

interface BehandlingRepository {
    fun finn(behandlingId: BehandlingId): Behandling?

    fun lagre(behandling: Behandling)
}
