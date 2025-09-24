package no.nav.helse.bakrommet.person

import io.ktor.server.application.*

// Extension function for PersonDao using the generic medIdent from common
suspend inline fun ApplicationCall.medIdent(
    personDao: PersonDao,
    crossinline block: suspend (naturligIdent: String, personId: SpilleromPersonId) -> Unit,
) = medIdent({ personDao.finnNaturligIdent(it) }, block)
