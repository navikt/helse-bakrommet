package no.nav.helse.bakrommet.errorhandling

import io.ktor.http.*
import io.ktor.server.application.*
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
                    detail = cause.message,
                    typePath = "validation/input",
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
                    instance = call.request.uri,
                ),
            )
            logg.error("Uventet feil", cause)
        }
    }
}
