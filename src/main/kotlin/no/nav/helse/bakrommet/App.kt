package no.nav.helse.bakrommet

import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

val appLogger = LoggerFactory.getLogger("bakrommet")

fun main() {
    appLogger.info("Hello, world!")
    embeddedServer(CIO, port = 8080) {
        routing {
            get("/isready") {
                call.respondText("I'm ready")
            }
            get("/isalive") {
                call.respondText("I'm alive")
            }
        }
    }.start(true)
}
