package no.nav.helse.bakrommet.auth

class SpilleromBearerToken(
    private val token: String,
) {
    suspend fun exchangeWithObo(
        tokenUtvekslingProvider: TokenUtvekslingProvider,
        scope: OAuthScope,
    ): OboToken {
        // her kan ev. caching ev this.token+scope -> oboToken være.
        return tokenUtvekslingProvider.exchangeToken(token, scope)
    }
}

class OAuthScope(
    val baseValue: String,
) {
    init {
        require(!baseValue.contains("api://") && !baseValue.contains("/.default")) {
            "Vennligst oppgi scope uten 'api://' og '/.default'"
        }
    }

    fun asDefaultScope(): String = "api://$baseValue/.default"
}
