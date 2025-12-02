package no.nav.helse.bakrommet.bruker

import io.ktor.server.routing.RoutingCall
import no.nav.helse.bakrommet.auth.Bruker
import no.nav.helse.bakrommet.auth.brukerPrincipal

class BrukerService {
    suspend fun hentBruker(call: RoutingCall): Bruker = call.brukerPrincipal() ?: throw IllegalStateException("Bruker ikke funnet i request")
}
