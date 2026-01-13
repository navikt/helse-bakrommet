package no.nav.helse.bakrommet.api.auth

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal
import io.ktor.server.routing.RoutingCall
import no.nav.helse.bakrommet.auth.BrukerOgToken
import no.nav.helse.bakrommet.domain.Bruker

fun ApplicationCall.brukerPrincipal(): Bruker? = principal<Bruker>()

fun RoutingCall.saksbehandler() = brukerPrincipal()!!

fun RoutingCall.saksbehandlerOgToken() =
    BrukerOgToken(
        bruker = saksbehandler(),
        token = request.bearerToken(),
    )
