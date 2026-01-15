package no.nav.helse.bakrommet.repository

import no.nav.helse.bakrommet.domain.person.NaturligIdent
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.Behandling
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.BehandlingId

interface BehandlingRepository {
    fun finn(behandlingId: BehandlingId): Behandling?

    fun hent(behandlingId: BehandlingId): Behandling = finn(behandlingId) ?: error("Fant ingen behandling med id=${behandlingId.value}")

    fun finnAlle(): List<Behandling>

    fun finnFor(naturligIdent: NaturligIdent): List<Behandling>

    fun lagre(behandling: Behandling)
}
