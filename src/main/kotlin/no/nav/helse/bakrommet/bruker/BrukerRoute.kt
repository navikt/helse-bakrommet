package no.nav.helse.bakrommet.bruker

import io.ktor.http.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.bakrommet.Configuration
import no.nav.helse.bakrommet.auth.Rolle
import no.nav.helse.bakrommet.auth.tilRoller
import no.nav.helse.bakrommet.util.saksbehandler
import no.nav.helse.bakrommet.util.serialisertTilString

data class Bruker(
    val navn: String,
    val navIdent: String,
    val preferredUsername: String,
    val roller: Set<Rolle>,
)

internal fun Route.brukerRoute(rolleConfig: Configuration.Roller) {
    get("/v1/bruker") {
        val principal = call.principal<JWTPrincipal>()
        val saksbehandler = call.saksbehandler()
        val roller = principal!!.tilRoller(rolleConfig)
        val bruker =
            Bruker(
                navn = saksbehandler.navn,
                navIdent = saksbehandler.navIdent,
                preferredUsername = saksbehandler.preferredUsername,
                roller = roller,
            )

        call.respondText(bruker.serialisertTilString(), ContentType.Application.Json, HttpStatusCode.OK)
    }
}
