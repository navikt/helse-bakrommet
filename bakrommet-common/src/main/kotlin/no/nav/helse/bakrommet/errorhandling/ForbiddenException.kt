package no.nav.helse.bakrommet.errorhandling

import io.ktor.server.application.*

class ForbiddenException(
    message: String,
) : ProblemDetailsException(
        message = message,
    ) {
    override fun toProblemDetails(call: ApplicationCall): ProblemDetails =
        ProblemDetails(
            status = 403,
            title = "Ingen tilgang",
            detail = message,
        )
}
