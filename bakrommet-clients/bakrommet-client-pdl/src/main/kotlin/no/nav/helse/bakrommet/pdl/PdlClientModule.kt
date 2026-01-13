package no.nav.helse.bakrommet.pdl

import no.nav.helse.bakrommet.auth.OAuthScope

object PdlClientModule {
    data class Configuration(
        val hostname: String,
        val scope: OAuthScope,
    )
}
