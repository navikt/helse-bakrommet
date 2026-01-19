package no.nav.helse.bakrommet.domain

import no.nav.helse.bakrommet.domain.saksbehandling.behandling.Behandling

class Bruker(
    val navn: String,
    val navIdent: String,
    val preferredUsername: String,
    val roller: Set<Rolle>,
) {
    fun erTildelt(behandling: Behandling): Boolean = behandling.opprettetAvNavIdent == navIdent
}
