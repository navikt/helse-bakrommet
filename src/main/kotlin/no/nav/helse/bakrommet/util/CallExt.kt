package no.nav.helse.bakrommet.util

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import no.nav.helse.bakrommet.nav.Saksbehandler

fun ApplicationCall.saksbehandler() =
    requireNotNull(principal<JWTPrincipal>()) {
        "Principal må være definert"
    }.tilSaksbehandler()

private fun JWTPrincipal.tilSaksbehandler(): Saksbehandler {
    return Saksbehandler(hentVerdi(verdi = "NAVident"), hentVerdi("name"), hentVerdi("preferred_username"))
}

private fun JWTPrincipal.hentVerdi(verdi: String) = requireNotNull(this[verdi]) { "$verdi må ha verdi i principal" }
