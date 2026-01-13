package no.nav.helse.bakrommet.auth

@JvmInline
value class OboToken(
    val value: String,
)

interface TokenUtvekslingProvider {
    suspend fun exchangeToken(
        accessToken: AccessToken,
        scope: OAuthScope,
    ): OboToken
}
