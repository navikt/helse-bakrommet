package no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt

import no.nav.helse.bakrommet.saksbehandlingsperiode.Saksbehandlingsperiode
import no.nav.helse.flex.sykepengesoknad.kafka.SykepengesoknadDTO

fun skapDagoversiktFraSoknader(
    søknader: List<SykepengesoknadDTO>,
    saksbehandlingsperiode: Saksbehandlingsperiode,
): List<Dag> {
    return emptyList()
}
