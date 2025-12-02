package no.nav.helse.bakrommet.person

import io.ktor.server.application.*

// Extension function for PersonDao using the generic medIdent from common
suspend inline fun ApplicationCall.medIdent(
    personService: PersonService,
    crossinline block: suspend (naturligIdent: String, personId: SpilleromPersonId) -> Unit,
) {
    medIdent({ personService.finnNaturligIdent(it) }, block)
}
