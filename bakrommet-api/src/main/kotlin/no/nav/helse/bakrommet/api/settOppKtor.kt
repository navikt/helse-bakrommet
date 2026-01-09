package no.nav.helse.bakrommet.api

import io.ktor.server.application.Application
import no.nav.helse.bakrommet.AuthOgRollerConfig
import no.nav.helse.bakrommet.Services
import no.nav.helse.bakrommet.auth.azureAdAppAuthentication

fun Application.settOppKtor(
    authOgRollerConfig: AuthOgRollerConfig,
    services: Services,
    errorHandlingIncludeStackTrace: Boolean = false,
) {
    azureAdAppAuthentication(authOgRollerConfig.auth, authOgRollerConfig.roller)
    helsesjekker()

    appModul(
        services = services,
        errorHandlingIncludeStackTrace = errorHandlingIncludeStackTrace,
    )
}
