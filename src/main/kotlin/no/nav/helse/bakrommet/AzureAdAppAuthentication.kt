package no.nav.helse.bakrommet

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import io.ktor.server.application.Application
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import java.net.URI

fun Application.azureAdAppAuthentication(config: Configuration.Auth) {
    azureAdAppAuthentication(
        jwkProvider = JwkProviderBuilder(URI(config.jwkProviderUri).toURL()).build(),
        issuerUrl = config.issuerUrl,
        clientId = config.clientId,
    )
}

fun Application.azureAdAppAuthentication(
    jwkProvider: JwkProvider,
    issuerUrl: String,
    clientId: String,
) {
    authentication {
        jwt("oidc") {
            verifier(jwkProvider, issuerUrl) {
                withAudience(clientId)
            }
            validate { credentials ->
                JWTPrincipal(credentials.payload)
            }
        }
    }
}
