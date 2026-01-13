package no.nav.helse.bakrommet.api.auth

import io.ktor.server.routing.RoutingRequest
import no.nav.helse.bakrommet.auth.AccessToken

fun RoutingRequest.bearerToken(): AccessToken {
    val authHeader = headers["Authorization"]!!
    val token = authHeader.removePrefix("Bearer ").trim()
    return AccessToken(token)
}
