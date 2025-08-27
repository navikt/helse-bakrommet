package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.util.objectMapper
import no.nav.helse.bakrommet.util.serialisertTilString
import org.slf4j.LoggerFactory

internal fun Route.demoUtbetalingsberegningRoute() {
    val logger = LoggerFactory.getLogger("DemoUtbetalingsberegningRoute")

    route("/api/demo/utbetalingsberegning") {
        /** Demo utbetalingsberegning - åpen endpoint */
        post {
            // Log rå input som tekst
            val rawInput = call.receiveText()
            try {
                // Parse input til objekt
                val input = no.nav.helse.bakrommet.util.objectMapper.readValue(rawInput, UtbetalingsberegningInput::class.java)
                val beregningData = UtbetalingsberegningLogikk.beregn(input)

                call.respondText(
                    beregningData.serialisertTilString(),
                    ContentType.Application.Json,
                    HttpStatusCode.OK,
                )
            } catch (e: Exception) {
                logger.error("Feil i demo utbetalingsberegning API", e)
                logger.info("Demo utbetalingsberegning input: $rawInput")

                call.respondText(
                    """{"error": "${e.message}"}""",
                    ContentType.Application.Json,
                    HttpStatusCode.BadRequest,
                )
            }
        }
    }
}
