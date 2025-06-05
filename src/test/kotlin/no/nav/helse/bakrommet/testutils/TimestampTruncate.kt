package no.nav.helse.bakrommet.testutils

import no.nav.helse.bakrommet.saksbehandlingsperiode.Dokument
import no.nav.helse.bakrommet.saksbehandlingsperiode.Saksbehandlingsperiode
import java.time.temporal.ChronoUnit.NANOS
import java.time.temporal.ChronoUnit.SECONDS

fun Saksbehandlingsperiode.truncateTidspunkt() = copy(opprettet = opprettet.truncatedTo(SECONDS))

fun Iterable<Dokument>.tidsstuttet() = map(Dokument::tidsstuttet)

fun Dokument.tidsstuttet() = copy(opprettet = opprettet.truncatedTo(NANOS))
