package no.nav.helse.bakrommet.util

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import no.nav.helse.bakrommet.Configuration
import no.nav.helse.bakrommet.nav.Saksbehandler

fun ApplicationCall.saksbehandler() =
    requireNotNull(principal<JWTPrincipal>()) {
        "Principal må være definert"
    }.tilSaksbehandler()

private fun JWTPrincipal.tilSaksbehandler() = Saksbehandler(hentVerdi("NAVident"), hentVerdi("name"))

private fun JWTPrincipal.hentVerdi(verdi: String) = requireNotNull(this[verdi]) { "$verdi må ha verdi i principal" }

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
