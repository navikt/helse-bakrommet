package no.nav.helse.bakrommet.util

import io.ktor.server.routing.*
import no.nav.helse.bakrommet.auth.OboClient
import no.nav.helse.bakrommet.auth.OboToken

class SpilleromBearerToken(private val token: String) {
    suspend fun exchangeWithObo(
        oboClient: OboClient,
        scope: String,
    ): OboToken {
        // her kan ev. caching ev this.token+scope -> oboToken være.
        return oboClient.exchangeToken(token, scope)
    }
}

fun RoutingRequest.bearerToken(): SpilleromBearerToken {
    val authHeader = headers["Authorization"]!!
    val token = authHeader.removePrefix("Bearer ").trim()
    return SpilleromBearerToken(token)
}
