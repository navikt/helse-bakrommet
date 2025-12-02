package no.nav.helse.bakrommet.person

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.auth.saksbehandlerOgToken

fun Route.personsøkRoute(
    service: PersonsøkService,
) {
    post("/v1/personsok") {
        val ident = call.receive<JsonNode>()["ident"].asText()
        val newPersonId = service.hentEllerOpprettPersonid(ident, call.saksbehandlerOgToken())
        call.response.headers.append("Content-Type", "application/json")
        call.respondText("""{ "personId": "$newPersonId" }""")
    }
}
