package no.nav.helse.bakrommet.auth

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal
import io.ktor.server.routing.RoutingCall

data class Bruker(
    val navn: String,
    val navIdent: String,
    val preferredUsername: String,
    val roller: Set<Rolle>,
)

fun ApplicationCall.brukerPrincipal(): Bruker? {
    return principal<Bruker>()
}

class BrukerOgToken(
    val bruker: Bruker,
    val token: SpilleromBearerToken,
)

fun RoutingCall.saksbehandler() = brukerPrincipal()!!

fun RoutingCall.saksbehandlerOgToken() =
    BrukerOgToken(
        bruker = saksbehandler(),
        token = request.bearerToken(),
    )
