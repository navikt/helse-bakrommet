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
                val input = objectMapper.readValue(rawInput, UtbetalingsberegningInput::class.java)
                val beregnet = UtbetalingsberegningLogikk.beregnAlaSpleis(input)
                val oppdrag = byggOppdragFraBeregning(beregnet, input.yrkesaktivitet, "NATURLIG_IDENT_DEMO")
                val beregningData = BeregningData(beregnet, oppdrag)
                val beregningDataDto = beregningData.tilBeregningDataUtDto()

                call.respondText(
                    beregningDataDto.serialisertTilString(),
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
