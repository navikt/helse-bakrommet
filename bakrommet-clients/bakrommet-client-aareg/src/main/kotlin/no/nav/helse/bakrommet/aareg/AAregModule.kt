package no.nav.helse.bakrommet.aareg

import no.nav.helse.bakrommet.auth.OAuthScope

object AAregModule {
    data class Configuration(
        val hostname: String,
        val scope: OAuthScope,
    )
}
