package no.nav.helse.bakrommet.tidslinje

import io.ktor.server.routing.*
import no.nav.helse.bakrommet.PARAM_PERSONID
import no.nav.helse.bakrommet.personId
import no.nav.helse.bakrommet.serde.respondJson

internal fun Route.tidslinjeRoute(service: TidslinjeService) {
    route("/v1/{$PARAM_PERSONID}/tidslinje") {
        get {
            val rader = service.hentTidslinje(call.personId())
            call.respondJson(Tidslinje(rader = rader))
        }
    }
}
