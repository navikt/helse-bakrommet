package no.nav.helse.bakrommet

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.sessions

data class VelgBrukerRequest(
    val navIdent: String,
)

fun Route.demoBrukerRoute() {
    get("/v1/demo/brukere") {
        call.respondText(predefinerteBrukere.serialisertTilString(), ContentType.Application.Json, HttpStatusCode.OK)
    }

    post("/v1/demo/bruker") {
        val sessionIdFraCookie = call.sessions.get("bakrommet-demo-session") as String?
        if (sessionIdFraCookie == null) {
            call.respond(HttpStatusCode.BadRequest, "Ingen session funnet")
            return@post
        }

        val request = call.receive<VelgBrukerRequest>()
        val valgtBruker =
            predefinerteBrukere.find { it.navIdent == request.navIdent }
                ?: run {
                    call.respond(HttpStatusCode.NotFound, "Bruker ikke funnet")
                    return@post
                }

        sessionsBrukere[sessionIdFraCookie] = valgtBruker
        call.respondText(valgtBruker.serialisertTilString(), ContentType.Application.Json, HttpStatusCode.OK)
    }

    post("/v1/demo/session/nullstill") {
        val sessionIdFraCookie = call.sessions.get("bakrommet-demo-session") as String?
        if (sessionIdFraCookie != null) {
            sessionsDaoer.remove(sessionIdFraCookie)
            sessionsBrukere.remove(sessionIdFraCookie)
        }
        call.sessions.clear("bakrommet-demo-session")
        call.respond(HttpStatusCode.OK, "Session nullstilt")
    }
}
