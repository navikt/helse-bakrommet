package no.nav.helse.bakrommet.api

import io.ktor.server.application.Application
import no.nav.helse.bakrommet.Services
import no.nav.helse.bakrommet.api.auth.azureAdAppAuthentication

fun Application.settOppKtor(
    configuration: ApiModule.Configuration,
    services: Services,
    errorHandlingIncludeStackTrace: Boolean = false,
) {
    azureAdAppAuthentication(configuration.auth, configuration.roller)
    helsesjekker()

    appModul(
        services = services,
        errorHandlingIncludeStackTrace = errorHandlingIncludeStackTrace,
    )
}
