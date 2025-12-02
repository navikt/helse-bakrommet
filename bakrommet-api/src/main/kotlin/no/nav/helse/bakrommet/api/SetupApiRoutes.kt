package no.nav.helse.bakrommet.api

import io.ktor.server.routing.Route
import no.nav.helse.bakrommet.Services
import no.nav.helse.bakrommet.api.behandling.behandlingRoute
import no.nav.helse.bakrommet.api.bruker.brukerRoute
import no.nav.helse.bakrommet.api.dokumenter.dokumentRoute
import no.nav.helse.bakrommet.api.tidslinje.tidslinjeRoute
import no.nav.helse.bakrommet.api.tilkommen.tilkommenInntektRoute
import no.nav.helse.bakrommet.api.vilkaar.vilkårRoute

fun Route.setupApiRoutes(
    services: Services,
) {
    behandlingRoute(services.behandlingService)
    brukerRoute(services.brukerService)
    tidslinjeRoute(services.tidslinjeService)
    vilkårRoute(services.vilkårService)
    dokumentRoute(services.dokumentHenter)
    tilkommenInntektRoute(services.tilkommenInntektService)
}
