package no.nav.helse.bakrommet.api.auth

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.server.application.Application
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.jwt
import no.nav.helse.bakrommet.api.ApiModule
import no.nav.helse.bakrommet.domain.Bruker
import java.net.URI

fun Application.azureAdAppAuthentication(
    config: ApiModule.Configuration.Auth,
    rolleconfig: ApiModule.Configuration.Roller,
) {
    val jwkProvider = JwkProviderBuilder(URI(config.jwkProviderUri).toURL()).build()
    authentication {
        jwt("entraid") {
            verifier(jwkProvider, config.issuerUrl) {
                withAudience(config.clientId)
            }
            validate { credentials ->
                Bruker(
                    navn = credentials.payload.getClaim("name").asString(),
                    navIdent = credentials.payload.getClaim("NAVident").asString(),
                    preferredUsername = credentials.payload.getClaim("preferred_username").asString(),
                    roller =
                        credentials.payload
                            .getClaim("groups")
                            .asList(String::class.java)
                            .toSet()
                            .tilRoller(rolleconfig),
                )
            }
        }
    }
}
