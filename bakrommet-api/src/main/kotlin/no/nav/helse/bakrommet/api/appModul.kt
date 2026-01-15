package no.nav.helse.bakrommet.api

import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.path
import io.ktor.server.routing.routing
import no.nav.helse.bakrommet.Services
import no.nav.helse.bakrommet.api.auth.RolleMatrise
import no.nav.helse.bakrommet.api.errorhandling.installErrorHandling
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.infrastruktur.db.Repositories
import no.nav.helse.bakrommet.util.objectMapper
import no.nav.helse.bakrommet.util.sikkerLogger
import org.slf4j.event.Level

internal fun Application.appModul(
    services: Services,
    db: DbDaoer<Repositories>,
    errorHandlingIncludeStackTrace: Boolean = false,
) {
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(objectMapper))
    }

    install(CallLogging) {
        disableDefaultColors()
        logger = sikkerLogger
        level = Level.INFO
        filter { call -> call.request.path().let { it != "/isalive" && it != "/isready" } }
    }

    installErrorHandling(includeStackTrace = errorHandlingIncludeStackTrace)

    routing {
        authenticate("entraid") {
            install(RolleMatrise)
            setupApiRoutes(services, db)
        }
    }
}
