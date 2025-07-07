package no.nav.helse.bakrommet.auth

import io.ktor.server.auth.jwt.JWTPrincipal
import no.nav.helse.bakrommet.Configuration

enum class Rolle {
    LES,
    SAKSBEHANDLER,
    BESLUTTER,
}

fun JWTPrincipal.tilRoller(config: Configuration.Roller): Set<Rolle> {
    val grupper = this.payload.getClaim("groups").asList(String::class.java).toSet()
    val roller = mutableSetOf<Rolle>()
    if (config.les.intersect(grupper).isNotEmpty()) {
        roller.add(Rolle.LES)
    }
    if (config.saksbehandler.intersect(grupper).isNotEmpty()) {
        roller.add(Rolle.SAKSBEHANDLER)
    }
    if (config.beslutter.intersect(grupper).isNotEmpty()) {
        roller.add(Rolle.BESLUTTER)
    }
    return roller
}
