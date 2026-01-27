package no.nav.helse.bakrommet.auth

interface TokenUtvekslingProvider {
    suspend fun exchangeToken(
        accessToken: AccessToken,
        scope: OAuthScope,
    ): OboToken
}
