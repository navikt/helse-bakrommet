package no.nav.helse.bakrommet.sigrun

import no.nav.helse.bakrommet.auth.OAuthScope

object SigrunClientModule {
    data class Configuration(
        val baseUrl: String,
        val scope: OAuthScope,
    )
}
