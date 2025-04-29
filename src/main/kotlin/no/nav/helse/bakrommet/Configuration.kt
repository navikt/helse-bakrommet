package no.nav.helse.bakrommet

data class Configuration(
    val db: DB,
    val obo: OBO,
    val pdl: PDL,
    val auth: Auth,
) {
    data class DB(
        val jdbcUrl: String,
    )

    data class OBO(
        val url: String,
    )

    data class PDL(
        val hostname: String,
        val scope: String,
    )

    data class Auth(
        val clientId: String,
        val issuerUrl: String,
        val jwkProviderUri: String,
        val tokenEndpoint: String,
    )

    companion object {
        fun fromEnv(env: Map<String, String> = System.getenv()): Configuration {
            return Configuration(
                db = DB(jdbcUrl = env.getValue("DATABASE_JDBC_URL")),
                obo = OBO(url = env.getValue("NAIS_TOKEN_EXCHANGE_ENDPOINT")),
                pdl =
                    PDL(
                        scope = env.getValue("PDL_SCOPE"),
                        hostname = env.getValue("PDL_HOSTNAME"),
                    ),
                auth =
                    Auth(
                        clientId = env.getValue("AZURE_APP_CLIENT_ID"),
                        issuerUrl = env.getValue("AZURE_OPENID_CONFIG_ISSUER"),
                        jwkProviderUri = env.getValue("AZURE_OPENID_CONFIG_JWKS_URI"),
                        tokenEndpoint = env.getValue("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
                    ),
            )
        }
    }
}
