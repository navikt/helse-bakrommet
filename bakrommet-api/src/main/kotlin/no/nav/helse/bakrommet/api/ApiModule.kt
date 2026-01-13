package no.nav.helse.bakrommet.api

object ApiModule {
    data class Configuration(
        val auth: Auth,
        val roller: Roller,
    ) {
        data class Auth(
            val clientId: String,
            val issuerUrl: String,
            val jwkProviderUri: String,
            val tokenEndpoint: String,
        )

        data class Roller(
            val les: Set<String>,
            val saksbehandler: Set<String>,
            val beslutter: Set<String>,
        )
    }
}
