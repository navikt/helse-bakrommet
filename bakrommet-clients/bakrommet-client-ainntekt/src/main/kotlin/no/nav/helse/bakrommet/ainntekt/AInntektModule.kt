package no.nav.helse.bakrommet.ainntekt

import no.nav.helse.bakrommet.auth.OAuthScope
import no.nav.helse.bakrommet.client.common.ApplicationConfig

object AInntektModule {
    data class Configuration(
        val hostname: String,
        val scope: OAuthScope,
        val appConfig: ApplicationConfig,
    )
}
