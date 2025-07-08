package no.nav.helse.bakrommet.auth

import no.nav.helse.bakrommet.Configuration

enum class Rolle {
    LES,
    SAKSBEHANDLER,
    BESLUTTER,
}

fun Set<String>.tilRoller(config: Configuration.Roller): Set<Rolle> {
    val roller = mutableSetOf<Rolle>()
    if (config.les.intersect(this).isNotEmpty()) {
        roller.add(Rolle.LES)
    }
    if (config.saksbehandler.intersect(this).isNotEmpty()) {
        roller.add(Rolle.SAKSBEHANDLER)
    }
    if (config.beslutter.intersect(this).isNotEmpty()) {
        roller.add(Rolle.BESLUTTER)
    }
    return roller
}
