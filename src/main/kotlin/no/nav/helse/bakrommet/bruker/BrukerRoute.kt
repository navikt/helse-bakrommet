package no.nav.helse.bakrommet.bruker

import io.ktor.http.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.*
import io.ktor.server.routing.*

internal fun Route.brukerRoute() {
    get("/v1/bruker") {
        val principal = call.principal<JWTPrincipal>()
        val claimsMap =
            buildMap {
                for (claim in principal!!.payload.claims) {
                    val value =
                        when {
                            claim.value.isNull -> null
                            claim.value.isMissing -> null
                            else -> {
                                claim.value.asString()
                                    ?: claim.value.asInt()
                                    ?: claim.value.asLong()
                                    ?: claim.value.asDouble()
                                    ?: claim.value.asBoolean()
                                    ?: claim.value.asList(String::class.java)
                                    ?: claim.value.toString()
                            }
                        }
                    put(claim.key, value)
                }
            }
        call.respond(HttpStatusCode.OK, claimsMap)
    }
}
