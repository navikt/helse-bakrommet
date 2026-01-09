package no.nav.helse.bakrommet.api.bruker

import io.ktor.server.routing.*
import no.nav.helse.bakrommet.api.serde.respondJson
import no.nav.helse.bakrommet.auth.brukerPrincipal

fun Route.brukerRoute() {
    get("/v1/bruker") {
        val bruker = call.brukerPrincipal() ?: throw IllegalStateException("Bruker ikke funnet i request")
        call.respondJson(bruker.tilBrukerDto())
    }
}
