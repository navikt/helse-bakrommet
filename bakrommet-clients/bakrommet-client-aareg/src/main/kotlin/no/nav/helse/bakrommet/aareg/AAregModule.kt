package no.nav.helse.bakrommet.aareg

import no.nav.helse.bakrommet.auth.OAuthScope
import no.nav.helse.bakrommet.client.common.ApplicationConfig

object AAregModule {
    data class Configuration(
        val hostname: String,
        val scope: OAuthScope,
        val appConfig: ApplicationConfig,
    )
}
