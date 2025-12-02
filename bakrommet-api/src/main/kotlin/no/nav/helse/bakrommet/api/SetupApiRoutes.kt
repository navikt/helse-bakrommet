package no.nav.helse.bakrommet.api

import io.ktor.server.routing.Route
import no.nav.helse.bakrommet.Services
import no.nav.helse.bakrommet.api.tidslinje.tidslinjeRoute

fun Route.setupApiRoutes(
    services: Services,
) {
    tidslinjeRoute(services.tidslinjeService)
}
