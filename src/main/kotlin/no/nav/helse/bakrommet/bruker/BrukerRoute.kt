package no.nav.helse.bakrommet.bruker

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.auth.brukerPrincipal
import no.nav.helse.bakrommet.util.serialisertTilString

internal fun Route.brukerRoute() {
    get("/v1/bruker") {
        val bruker = call.brukerPrincipal()

        call.respondText(bruker.serialisertTilString(), ContentType.Application.Json, HttpStatusCode.OK)
    }
}
