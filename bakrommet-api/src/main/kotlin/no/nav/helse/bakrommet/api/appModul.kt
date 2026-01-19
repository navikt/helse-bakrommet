package no.nav.helse.bakrommet.api

import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.Providers
import no.nav.helse.bakrommet.Services
import no.nav.helse.bakrommet.api.auth.RolleMatrise
import no.nav.helse.bakrommet.api.errorhandling.installErrorHandling
import no.nav.helse.bakrommet.infrastruktur.db.AlleDaoer
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer
import no.nav.helse.bakrommet.objectMapper
import no.nav.helse.bakrommet.sikkerLogger
import org.slf4j.event.Level

internal fun Application.appModul(
    providers: Providers,
    services: Services,
    db: DbDaoer<AlleDaoer>,
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
            setupApiRoutes(services, db, providers)
        }
    }
}
