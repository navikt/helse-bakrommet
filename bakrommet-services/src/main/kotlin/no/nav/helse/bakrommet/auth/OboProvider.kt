package no.nav.helse.bakrommet.auth

class OboToken(
    private val value: String,
) {
    fun somBearerHeader() = "Bearer $value"
}

interface TokenUtvekslingProvider {
    suspend fun exchangeToken(
        bearerToken: String,
        scope: OAuthScope,
    ): OboToken
}
