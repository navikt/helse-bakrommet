package no.nav.helse.bakrommet.testutils

import no.nav.helse.bakrommet.saksbehandlingsperiode.Saksbehandlingsperiode

fun Saksbehandlingsperiode.truncateTidspunkt(): Saksbehandlingsperiode {
    return this.copy(
        opprettet = this.opprettet.truncatedTo(java.time.temporal.ChronoUnit.SECONDS),
    )
}
