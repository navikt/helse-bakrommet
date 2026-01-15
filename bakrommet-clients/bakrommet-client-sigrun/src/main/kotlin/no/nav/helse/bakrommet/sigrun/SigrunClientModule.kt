package no.nav.helse.bakrommet.sigrun

import no.nav.helse.bakrommet.auth.OAuthScope
import no.nav.helse.bakrommet.client.common.ApplicationConfig

object SigrunClientModule {
    data class Configuration(
        val baseUrl: String,
        val scope: OAuthScope,
        val appConfig: ApplicationConfig,
    )
}
