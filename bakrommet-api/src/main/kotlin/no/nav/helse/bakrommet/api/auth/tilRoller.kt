package no.nav.helse.bakrommet.api.auth

import no.nav.helse.bakrommet.api.ApiModule
import no.nav.helse.bakrommet.domain.Rolle

internal fun Set<String>.tilRoller(config: ApiModule.Configuration.Roller): Set<Rolle> {
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
