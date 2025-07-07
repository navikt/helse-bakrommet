package no.nav.helse.bakrommet.bruker

import io.ktor.http.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.util.serialisertTilString

internal fun Route.brukerRoute() {
    get("/v1/bruker") {
        val principal = call.principal<JWTPrincipal>()
        val claims = principal!!.payload.claims.serialisertTilString()
        call.respondText(claims, ContentType.Application.Json, HttpStatusCode.OK)
    }
}
