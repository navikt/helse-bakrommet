package no.nav.helse.bakrommet.sykepengesoknad

import no.nav.helse.bakrommet.auth.OAuthScope

object SykepengesøknadBackendClientModule {
    data class Configuration(
        val hostname: String,
        val scope: OAuthScope,
    )
}
