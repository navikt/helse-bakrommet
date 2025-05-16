package no.nav.helse.bakrommet.errorhandling

import io.ktor.server.application.*

class SoknadIkkeFunnetException(message: String) : ProblemDetailsException(
    message,
) {
    override fun toProblemDetails(call: ApplicationCall): ProblemDetails {
        return ProblemDetails(
            status = 404,
            title = "Søknad ikke funnet",
            detail = message,
        )
    }
}
