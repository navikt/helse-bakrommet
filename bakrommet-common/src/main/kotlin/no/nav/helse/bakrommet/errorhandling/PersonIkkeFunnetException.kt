package no.nav.helse.bakrommet.errorhandling

import io.ktor.server.application.*

class PersonIkkeFunnetException :
    ProblemDetailsException(
        message = "Fant ikke person i PDL",
    ) {
    override fun toProblemDetails(call: ApplicationCall): ProblemDetails =
        ProblemDetails(
            status = 404,
            title = "Person ikke funnet",
            detail = message,
        )
}
