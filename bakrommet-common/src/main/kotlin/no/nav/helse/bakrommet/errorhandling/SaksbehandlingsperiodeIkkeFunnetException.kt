package no.nav.helse.bakrommet.errorhandling

import io.ktor.server.application.*

class SaksbehandlingsperiodeIkkeFunnetException :
    ProblemDetailsException(
        message = "Fant ikke saksbehandlingsperiode",
    ) {
    override fun toProblemDetails(call: ApplicationCall): ProblemDetails =
        ProblemDetails(
            status = 404,
            title = "Saksbehandlingsperiode ikke funnet",
            detail = message,
        )
}
