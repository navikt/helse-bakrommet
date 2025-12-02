package no.nav.helse.bakrommet.api.tidslinje

import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.helse.bakrommet.PARAM_PERSONID
import no.nav.helse.bakrommet.api.serde.respondJson
import no.nav.helse.bakrommet.personId
import no.nav.helse.bakrommet.tidslinje.Tidslinje
import no.nav.helse.bakrommet.tidslinje.TidslinjeService

fun Route.tidslinjeRoute(service: TidslinjeService) {
    route("/v1/{$PARAM_PERSONID}/tidslinje") {
        get {
            val tidslinje = service.hentTidslinje(call.personId())
            val response = Tidslinje(tidslinje).tilTidslinjeV1Dto()
            call.respondJson(response)
        }
    }

    route("/v2/{$PARAM_PERSONID}/tidslinje") {
        get {
            val response = service.hentTidslinjeData(call.personId()).tilTidslinjeDto()
            call.respondJson(response)
        }
    }
}
