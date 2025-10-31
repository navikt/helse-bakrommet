package no.nav.helse.bakrommet.person

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import no.nav.helse.bakrommet.PARAM_PERSONID

suspend inline fun ApplicationCall.medIdent(
    crossinline finnNaturligIdent: suspend (personId: String) -> String?,
    crossinline block: suspend (naturligIdent: String, personId: SpilleromPersonId) -> Unit,
) {
    val personId =
        parameters[PARAM_PERSONID]
            ?: return respond(HttpStatusCode.BadRequest, "Mangler personId i path")

    val fnr =
        finnNaturligIdent(personId)
            ?: return respond(HttpStatusCode.NotFound, "Fant ikke naturligIdent for personId $personId")

    block(fnr, SpilleromPersonId(personId))
}

// Interface that PersonDao can implement
interface PersonIdLookup {
    fun finnNaturligIdent(spilleromId: String): String?
}
