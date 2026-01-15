package no.nav.helse.bakrommet.inntektsmelding

import no.nav.helse.bakrommet.auth.OAuthScope
import no.nav.helse.bakrommet.client.common.ApplicationConfig

object InntektsmeldingClientModule {
    data class Configuration(
        val baseUrl: String,
        val scope: OAuthScope,
        val appConfig: ApplicationConfig,
    )
}
