package no.nav.helse.bakrommet.api.auth

import io.ktor.server.routing.RoutingRequest
import no.nav.helse.bakrommet.auth.SpilleromBearerToken

fun RoutingRequest.bearerToken(): SpilleromBearerToken {
    val authHeader = headers["Authorization"]!!
    val token = authHeader.removePrefix("Bearer ").trim()
    return SpilleromBearerToken(token)
}
