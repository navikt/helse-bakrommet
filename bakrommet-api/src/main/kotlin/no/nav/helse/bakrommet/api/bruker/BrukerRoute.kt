package no.nav.helse.bakrommet.api.bruker

import io.ktor.server.routing.*
import no.nav.helse.bakrommet.api.serde.respondJson
import no.nav.helse.bakrommet.bruker.BrukerService

fun Route.brukerRoute(service: BrukerService) {
    get("/v1/bruker") {
        val bruker = service.hentBruker(call)
        call.respondJson(bruker.tilBrukerDto())
    }
}
