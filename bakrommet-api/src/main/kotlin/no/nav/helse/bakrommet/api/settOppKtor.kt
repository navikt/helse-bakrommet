package no.nav.helse.bakrommet.api

import io.ktor.server.application.Application
import no.nav.helse.bakrommet.Services
import no.nav.helse.bakrommet.api.auth.azureAdAppAuthentication
import no.nav.helse.bakrommet.infrastruktur.db.AlleDaoer
import no.nav.helse.bakrommet.infrastruktur.db.DbDaoer

fun Application.settOppKtor(
    configuration: ApiModule.Configuration,
    services: Services,
    db: DbDaoer<AlleDaoer>,
    errorHandlingIncludeStackTrace: Boolean = false,
) {
    azureAdAppAuthentication(configuration.auth, configuration.roller)
    helsesjekker()

    appModul(
        services = services,
        errorHandlingIncludeStackTrace = errorHandlingIncludeStackTrace,
        db = db,
    )
}
