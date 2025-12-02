package no.nav.helse.bakrommet.api.tidslinje

import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.helse.bakrommet.PARAM_PERSONID
import no.nav.helse.bakrommet.api.dto.tidslinje.TidslinjeDto
import no.nav.helse.bakrommet.api.serde.respondJson
import no.nav.helse.bakrommet.personId
import no.nav.helse.bakrommet.tidslinje.TidslinjeService

data class TidslinjeRoute(
    val beløp: Int,
)

fun Route.tidslinjeRoute(service: TidslinjeService) {
    route("/v2/{$PARAM_PERSONID}/tidslinje") {
        get {
            val rader = service.hentTidslinje(call.personId())
            call.respondJson(TidslinjeDto(234))
        }
    }
}
