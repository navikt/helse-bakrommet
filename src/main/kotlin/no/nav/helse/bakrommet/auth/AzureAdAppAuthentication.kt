package no.nav.helse.bakrommet.auth

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.server.application.Application
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import no.nav.helse.bakrommet.Configuration
import java.net.URI

fun Application.azureAdAppAuthentication(config: Configuration.Auth) {
    val jwkProvider = JwkProviderBuilder(URI(config.jwkProviderUri).toURL()).build()
    authentication {
        jwt("entraid") {
            verifier(jwkProvider, config.issuerUrl) {
                withAudience(config.clientId)
            }
            validate { credentials ->
                JWTPrincipal(credentials.payload)
            }
        }
    }
}
