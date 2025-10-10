package no.nav.helse.bakrommet

import no.nav.helse.bakrommet.auth.OAuthScope

data class Configuration(
    val db: DB,
    val obo: OBO,
    val pdl: PDL,
    val auth: Auth,
    val sykepengesoknadBackend: SykepengesoknadBackend,
    val aareg: AAReg,
    val ainntekt: AInntekt,
    val inntektsmelding: Inntektsmelding,
    val sigrun: Sigrun,
    val roller: Roller,
    val naisClusterName: String,
) {
    data class DB(
        val jdbcUrl: String,
    )

    data class OBO(
        val url: String,
    )

    data class PDL(
        val hostname: String,
        val scope: OAuthScope,
    )

    data class AAReg(
        val hostname: String,
        val scope: OAuthScope,
    )

    data class AInntekt(
        val hostname: String,
        val scope: OAuthScope,
    )

    data class Sigrun(
        val baseUrl: String,
        val scope: OAuthScope,
    )

    data class SykepengesoknadBackend(
        val hostname: String,
        val scope: OAuthScope,
    )

    data class Inntektsmelding(
        val baseUrl: String,
        val scope: OAuthScope,
    )

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

    companion object {
        private fun String.asSet() = this.split(",").map { it.trim() }.toSet()

        fun fromEnv(env: Map<String, String> = System.getenv()): Configuration =
            Configuration(
                roller =
                    Roller(
                        les = env.getValue("ROLLE_GRUPPER_LES").asSet(),
                        saksbehandler = env.getValue("ROLLE_GRUPPER_SAKSBEHANDLER").asSet(),
                        beslutter = env.getValue("ROLLE_GRUPPER_BESLUTTER").asSet(),
                    ),
                db = DB(jdbcUrl = env.getValue("DATABASE_JDBC_URL")),
                obo = OBO(url = env.getValue("NAIS_TOKEN_EXCHANGE_ENDPOINT")),
                pdl =
                    PDL(
                        scope = OAuthScope(env.getValue("PDL_SCOPE")),
                        hostname = env.getValue("PDL_HOSTNAME"),
                    ),
                auth =
                    Auth(
                        clientId = env.getValue("AZURE_APP_CLIENT_ID"),
                        issuerUrl = env.getValue("AZURE_OPENID_CONFIG_ISSUER"),
                        jwkProviderUri = env.getValue("AZURE_OPENID_CONFIG_JWKS_URI"),
                        tokenEndpoint = env.getValue("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
                    ),
                sykepengesoknadBackend =
                    SykepengesoknadBackend(
                        scope = OAuthScope(env.getValue("SYKEPENGESOKNAD_BACKEND_SCOPE")),
                        hostname = env.getValue("SYKEPENGESOKNAD_BACKEND_HOSTNAME"),
                    ),
                aareg =
                    AAReg(
                        scope = OAuthScope(env.getValue("AAREG_SCOPE")),
                        hostname = env.getValue("AAREG_HOSTNAME"),
                    ),
                ainntekt =
                    AInntekt(
                        scope = OAuthScope(env.getValue("INNTEKTSKOMPONENTEN_SCOPE")),
                        hostname = env.getValue("INNTEKTSKOMPONENTEN_HOSTNAME"),
                    ),
                inntektsmelding =
                    Inntektsmelding(
                        scope = OAuthScope(env.getValue("INNTEKTSMELDING_SCOPE")),
                        baseUrl = env.getValue("INNTEKTSMELDING_BASE_URL"),
                    ),
                sigrun =
                    Sigrun(
                        scope = OAuthScope(env.getValue("SIGRUN_SCOPE")),
                        baseUrl = env.getValue("SIGRUN_URL"),
                    ),
                naisClusterName = env.getValue("NAIS_CLUSTER_NAME"),
            )
    }
}
