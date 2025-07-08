package no.nav.helse.bakrommet.auth

import io.ktor.server.auth.principal
import io.ktor.server.routing.RoutingCall

data class Bruker(
    val navn: String,
    val navIdent: String,
    val preferredUsername: String,
    val roller: Set<Rolle>,
)

fun RoutingCall.brukerPrincipal(): Bruker {
    return principal<Bruker>() ?: throw IllegalStateException("Bruker må være autentisert")
}
