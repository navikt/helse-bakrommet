package no.nav.helse.bakrommet.errorhandling

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import no.nav.helse.bakrommet.Configuration
import no.nav.helse.bakrommet.util.logg

fun Application.installErrorHandling(configuration: Configuration) {
    install(StatusPages) {
        // Catch-all
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
