package no.nav.helse.bakrommet.auth

import io.ktor.server.routing.*

class SpilleromBearerToken(private val token: String) {
    suspend fun exchangeWithObo(
        oboClient: OboClient,
        scope: OAuthScope,
    ): OboToken {
        // her kan ev. caching ev this.token+scope -> oboToken være.
        return oboClient.exchangeToken(token, scope)
    }
}

class OAuthScope(val baseValue: String) {
    init {
        require(!baseValue.contains("api://") && !baseValue.contains("/.default")) {
            "Vennligst oppgi scope uten 'api://' og '/.default'"
        }
    }

    fun asDefaultScope(): String {
        return "api://$baseValue/.default"
    }
}

fun RoutingRequest.bearerToken(): SpilleromBearerToken {
    val authHeader = headers["Authorization"]!!
    val token = authHeader.removePrefix("Bearer ").trim()
    return SpilleromBearerToken(token)
}
