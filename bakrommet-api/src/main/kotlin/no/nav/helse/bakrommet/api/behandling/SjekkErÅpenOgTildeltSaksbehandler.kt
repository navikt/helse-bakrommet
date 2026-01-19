package no.nav.helse.bakrommet.api.behandling

import no.nav.helse.bakrommet.domain.Bruker
import no.nav.helse.bakrommet.domain.saksbehandling.behandling.Behandling

fun Behandling.sjekkErÅpenOgTildeltSaksbehandler(bruker: Bruker): Behandling {
    if (!this.erÅpenForEndringer()) {
        error("Kan ikke endre yrkesaktivitet på en lukket behandling")
    }
    if (!bruker.erTildelt(this)) {
        error("Saksbehandler har ikke tilgang til å endre denne behandlingen")
    }
    return this
}
