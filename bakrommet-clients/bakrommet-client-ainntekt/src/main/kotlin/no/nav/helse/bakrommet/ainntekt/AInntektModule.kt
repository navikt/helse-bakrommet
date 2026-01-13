package no.nav.helse.bakrommet.ainntekt

import no.nav.helse.bakrommet.auth.OAuthScope

object AInntektModule {
    data class Configuration(
        val hostname: String,
        val scope: OAuthScope,
    )
}
