package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.util.serialisertTilString

internal fun Route.demoUtbetalingsberegningRoute() {
    route("/api/demo/utbetalingsberegning") {
        /** Demo utbetalingsberegning - åpen endpoint */
        post {
            try {
                val input = call.receive<UtbetalingsberegningInput>()
                val beregningData = UtbetalingsberegningLogikk.beregn(input)

                call.respondText(
                    beregningData.serialisertTilString(),
                    ContentType.Application.Json,
                    HttpStatusCode.OK,
                )
            } catch (e: Exception) {
                call.respondText(
                    """{"error": "${e.message}"}""",
                    ContentType.Application.Json,
                    HttpStatusCode.BadRequest,
                )
            }
        }
    }
}
