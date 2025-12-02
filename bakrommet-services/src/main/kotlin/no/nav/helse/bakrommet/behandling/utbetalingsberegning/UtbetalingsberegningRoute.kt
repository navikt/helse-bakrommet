package no.nav.helse.bakrommet.behandling.utbetalingsberegning

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.PARAM_PERIODEUUID
import no.nav.helse.bakrommet.PARAM_PERSONID
import no.nav.helse.bakrommet.behandling.periodeReferanse
import no.nav.helse.bakrommet.util.serialisertTilString

internal fun Route.beregningRoute(service: UtbetalingsberegningService) {
    route("/v1/{$PARAM_PERSONID}/behandlinger/{$PARAM_PERIODEUUID}/utbetalingsberegning") {
        /** Hent eksisterende beregning */
        get {
            val beregning = service.hentUtbetalingsberegning(call.periodeReferanse())
            call.respondText(
                beregning?.tilBeregningResponseUtDto()?.serialisertTilString() ?: "null",
                ContentType.Application.Json,
                HttpStatusCode.OK,
            )
        }
    }
}
