package no.nav.helse.bakrommet.inntektsmelding

import no.nav.helse.bakrommet.auth.OAuthScope

object InntektsmeldingClientModule {
    data class Configuration(
        val baseUrl: String,
        val scope: OAuthScope,
    )
}
