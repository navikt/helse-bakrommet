package no.nav.helse.bakrommet.errorhandling

import io.ktor.server.application.*

class IkkeFunnetException(val title: String, detail: String = title) : ProblemDetailsException(
    detail,
) {
    override fun toProblemDetails(call: ApplicationCall): ProblemDetails {
        return ProblemDetails(
            status = 404,
            title = title,
            detail = message,
        )
    }
}
