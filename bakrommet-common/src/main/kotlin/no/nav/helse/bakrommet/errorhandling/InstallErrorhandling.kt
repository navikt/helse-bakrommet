package no.nav.helse.bakrommet.errorhandling

import io.ktor.http.*
import io.ktor.serialization.JsonConvertException
import io.ktor.server.application.*
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import no.nav.helse.bakrommet.Configuration
import no.nav.helse.bakrommet.util.logg

fun Application.installErrorHandling(configuration: Configuration) {
    install(StatusPages) {
        exception<InputValideringException> { call, cause ->
            val status = HttpStatusCode.BadRequest

            val problem =
                buildProblem(
                    status = status,
                    title = cause.message,
                    detail = null,
                    typePath = "validation/input",
                    instance = call.request.uri,
                )
            logg.warn("Input valideringsexception ${call.request.uri}", cause)

            call.respondProblem(status, problem)
        }
        exception<BadRequestException> { call, cause ->
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
        exception<ProblemDetailsException> { call, cause ->
            val problem = cause.toProblemDetails(call)
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
                        if (configuration.naisClusterName == "dev-gcp") {
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
