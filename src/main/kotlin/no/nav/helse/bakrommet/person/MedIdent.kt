package no.nav.helse.bakrommet.person

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import no.nav.helse.bakrommet.PARAM_PERSONID

internal suspend inline fun ApplicationCall.medIdent(
    personDao: PersonDao,
    crossinline block: suspend (naturligIdent: String, personId: SpilleromPersonId) -> Unit,
) {
    val personId =
        parameters[PARAM_PERSONID]
            ?: return respond(HttpStatusCode.BadRequest, "Mangler personId i path")

    val fnr =
        personDao.finnNaturligIdent(personId)
            ?: return respond(HttpStatusCode.NotFound, "Fant ikke naturligIdent for personId $personId")

    block(fnr, SpilleromPersonId(personId))
}
