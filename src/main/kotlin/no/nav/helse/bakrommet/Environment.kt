package no.nav.helse.bakrommet

fun authConfig() = System.getenv().let { env ->
    AuthConfiguration(
        clientId = env.getValue("AZURE_APP_CLIENT_ID"),
        issuerUrl = env.getValue("AZURE_OPENID_CONFIG_ISSUER"),
        jwkProviderUri = env.getValue("AZURE_OPENID_CONFIG_JWKS_URI"),
        tokenEndpoint = env.getValue("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
    )
}