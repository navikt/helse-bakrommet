package no.nav.helse.bakrommet.ereg

import no.nav.helse.bakrommet.client.common.ApplicationConfig

object EregClientModule {
    data class Configuration(
        val baseUrl: String,
        val appConfig: ApplicationConfig,
    )
}
