package no.nav.helse.bakrommet

import no.nav.helse.bakrommet.api.settOppKtor

fun main() {
    val configuration = Configuration.fromEnv()
    val dataSource = instansierDatabase(configuration.db)
    val clienter: Clienter = createClients(configuration)
    val services: Services = createServices(clienter, skapDbDaoer(dataSource))
    startApp(configuration) {
        settOppKtor(
            authOgRollerConfig = configuration,
            services = services,
            errorHandlingIncludeStackTrace = configuration.naisClusterName == "dev-gcp",
        )
    }
}
