package no.nav.helse.bakrommet.api.utbetalingsberegning

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.helse.bakrommet.api.PARAM_PERIODEUUID
import no.nav.helse.bakrommet.api.PARAM_PERSONID
import no.nav.helse.bakrommet.api.periodeReferanse
import no.nav.helse.bakrommet.api.serde.respondJson
import no.nav.helse.bakrommet.behandling.utbetalingsberegning.UtbetalingsberegningService
import no.nav.helse.bakrommet.person.PersonService

fun Route.beregningRoute(
    service: UtbetalingsberegningService,
    personService: PersonService,
) {
    route("/v1/{$PARAM_PERSONID}/behandlinger/{$PARAM_PERIODEUUID}/utbetalingsberegning") {
        /** Hent eksisterende beregning */
        get {
            val beregning = service.hentUtbetalingsberegning(call.periodeReferanse(personService))
            if (beregning != null) {
                call.respondJson(beregning.tilBeregningResponseDto())
            } else {
                // Returnerer null som JSON for å bevare oppførselen fra før
                call.respondText("null", ContentType.Application.Json, HttpStatusCode.OK)
            }
        }
    }
}
