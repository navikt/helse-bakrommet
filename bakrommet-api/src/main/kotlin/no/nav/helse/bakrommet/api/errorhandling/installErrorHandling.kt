package no.nav.helse.bakrommet.api.errorhandling

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.JsonConvertException
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.uri
import no.nav.helse.bakrommet.errorhandling.ApplicationException
import no.nav.helse.bakrommet.errorhandling.ForbiddenException
import no.nav.helse.bakrommet.errorhandling.IkkeFunnetException
import no.nav.helse.bakrommet.errorhandling.InputValideringException
import no.nav.helse.bakrommet.errorhandling.PersonIkkeFunnetException
import no.nav.helse.bakrommet.errorhandling.ProblemDetails
import no.nav.helse.bakrommet.errorhandling.SaksbehandlingsperiodeIkkeFunnetException
import no.nav.helse.bakrommet.errorhandling.SoknadIkkeFunnetException
import no.nav.helse.bakrommet.logg

fun Application.installErrorHandling(includeStackTrace: Boolean = false) {
    install(StatusPages) {
        val badRequestHandler: suspend (ApplicationCall, Throwable) -> Unit = { call, cause ->
            val status = HttpStatusCode.BadRequest
            logg.error("Bad request", cause)

            val problem =
                buildProblem(
                    status = status,
                    title = "Ugyldig forespørsel",
                    detail = cause.message,
                    typePath = "validation/request",
                    instance = call.request.uri,
                )

            call.respondProblem(status, problem)
        }
        exception<BadRequestException>(badRequestHandler)
        exception<IllegalArgumentException>(badRequestHandler)
        exception<JsonConvertException> { call, cause ->
            val status = HttpStatusCode.BadRequest

            val problem =
                buildProblem(
                    status = status,
                    title = "Ugyldig JSON-format",
                    detail = cause.message,
                    typePath = "validation/json",
                    instance = call.request.uri,
                )

            call.respondProblem(status, problem)
        }
        exception<ApplicationException> { call, cause ->
            val problem = cause.toProblemDetails(call)
            if (cause is InputValideringException) {
                logg.warn("Input valideringsexception ${call.request.uri}", cause)
            }
            val status = HttpStatusCode.fromValue(problem.status)
            call.respondProblem(status, problem)
        }

        exception<Throwable> { call, cause ->
            val status = HttpStatusCode.InternalServerError
            call.respondProblem(
                status,
                buildProblem(
                    status,
                    detail =
                        if (includeStackTrace) {
                            cause.stackTraceToString()
                        } else {
                            "Uventet serverfeil"
                        },
                    typePath = "errors/internal",
                    title = "Ukjent feil",
                    instance = call.request.uri,
                ),
            )
            logg.error("Uventet feil", cause)
        }
    }
}

private fun ApplicationException.toProblemDetails(call: ApplicationCall): ProblemDetails =
    when (this) {
        is ForbiddenException ->
            ProblemDetails(
                status = 403,
                title = "Ingen tilgang",
                detail = message,
            )

        is IkkeFunnetException ->
            ProblemDetails(
                status = 404,
                title = title,
                detail = message,
            )

        is PersonIkkeFunnetException ->
            ProblemDetails(
                status = 404,
                title = "Person ikke funnet",
                detail = message,
            )

        is SaksbehandlingsperiodeIkkeFunnetException ->
            ProblemDetails(
                status = 404,
                title = "Saksbehandlingsperiode ikke funnet",
                detail = message,
            )

        is SoknadIkkeFunnetException ->
            ProblemDetails(
                status = 404,
                title = "Søknad ikke funnet",
                detail = message,
            )

        is InputValideringException ->
            ProblemDetails(
                status = 400,
                title = message,
                type = "https://spillerom.ansatt.nav.no/validation/input",
                instance = call.request.uri,
            )
    }
