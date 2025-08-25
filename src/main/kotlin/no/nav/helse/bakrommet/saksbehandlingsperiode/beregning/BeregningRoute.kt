package no.nav.helse.bakrommet.saksbehandlingsperiode.beregning

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.PARAM_PERIODEUUID
import no.nav.helse.bakrommet.PARAM_PERSONID
import no.nav.helse.bakrommet.saksbehandlingsperiode.periodeReferanse
import no.nav.helse.bakrommet.util.serialisertTilString

internal fun Route.beregningRoute(service: BeregningService) {
    route("/v1/{$PARAM_PERSONID}/saksbehandlingsperioder/{$PARAM_PERIODEUUID}/beregning") {
        /** Hent eksisterende beregning */
        get {
            val beregning = service.hentBeregning(call.periodeReferanse())
            call.respondText(
                beregning?.serialisertTilString() ?: "null",
                ContentType.Application.Json,
                HttpStatusCode.OK,
            )
        }

        /** Slett beregning */
        delete {
            service.slettBeregning(call.periodeReferanse())
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
