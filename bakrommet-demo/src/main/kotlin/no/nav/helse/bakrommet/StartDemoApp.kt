package no.nav.helse.bakrommet

import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.errorhandling.installErrorHandling
import no.nav.helse.bakrommet.util.objectMapper
import no.nav.helse.bakrommet.util.sikkerLogger
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

// App-oppstarten må definere egen logger her, siden den (per nå) ikke skjer inne i en klasse
val appLogger: Logger = LoggerFactory.getLogger("bakrommet")

fun main() {
    embeddedServer(CIO, port = 8080) {
        appLogger.info("Setter opp ktor")

        helsesjekker()

        install(ContentNegotiation) {
            register(ContentType.Application.Json, JacksonConverter(objectMapper))
        }

        install(CallLogging) {
            disableDefaultColors()
            logger = sikkerLogger
            level = Level.INFO
            filter { call -> call.request.path().let { it != "/isalive" && it != "/isready" } }
        }

        installErrorHandling(true)

        val services = TODO()
        routing {
            setupRoutes(services)
        }

        appLogger.info("Starter bakrommet")
    }.start(true)
}
