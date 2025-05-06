package no.nav.helse.bakrommet.util

import io.ktor.server.routing.*

fun RoutingRequest.bearerToken(): String {
    val authHeader = headers["Authorization"]!!
    val token = authHeader.removePrefix("Bearer ").trim()
    return token
}
