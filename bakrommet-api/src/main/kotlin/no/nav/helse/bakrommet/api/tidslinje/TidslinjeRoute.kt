package no.nav.helse.bakrommet.api.tidslinje

import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.helse.bakrommet.api.PARAM_PSEUDO_ID
import no.nav.helse.bakrommet.api.naturligIdent
import no.nav.helse.bakrommet.api.serde.respondJson
import no.nav.helse.bakrommet.person.PersonService
import no.nav.helse.bakrommet.tidslinje.TidslinjeService

fun Route.tidslinjeRoute(
    service: TidslinjeService,
    personService: PersonService,
) {
    route("/v2/{$PARAM_PSEUDO_ID}/tidslinje") {
        get {
            val response = service.hentTidslinjeData(call.naturligIdent(personService)).tilTidslinjeDto()
            call.respondJson(response)
        }
    }
}
