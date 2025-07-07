package no.nav.helse.bakrommet.bruker

import io.ktor.http.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.*
import io.ktor.server.routing.*

internal fun Route.brukerRoute() {
    get("/v1/bruker") {
        val principal = call.principal<JWTPrincipal>()
        val claimsMap =
            buildMap {
                for (claim in principal!!.payload.claims) {
                    put(claim.key, claim.value.asString())
                }
            }
        call.respond(HttpStatusCode.OK, claimsMap)
    }
}
