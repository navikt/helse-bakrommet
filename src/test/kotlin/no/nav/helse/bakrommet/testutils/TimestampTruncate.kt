package no.nav.helse.bakrommet.testutils

import no.nav.helse.bakrommet.saksbehandlingsperiode.Saksbehandlingsperiode
import no.nav.helse.bakrommet.saksbehandlingsperiode.dokumenter.Dokument
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.Yrkesaktivitet
import java.time.temporal.ChronoUnit.MILLIS
import java.time.temporal.ChronoUnit.SECONDS

fun Saksbehandlingsperiode.truncateTidspunkt() = copy(opprettet = opprettet.truncatedTo(SECONDS))

@JvmName("tidsstuttetSaksbehandlingsperiode")
fun Iterable<Saksbehandlingsperiode>.tidsstuttet() = map(Saksbehandlingsperiode::truncateTidspunkt)

@JvmName("tidsstuttetDokument")
fun Iterable<Dokument>.tidsstuttet() = map(Dokument::tidsstuttet)

fun Dokument.tidsstuttet() = copy(opprettet = opprettet.truncatedTo(MILLIS))

@JvmName("tidsstuttetYrkesaktivitet")
fun Iterable<Yrkesaktivitet>.tidsstuttet() = map(Yrkesaktivitet::tidsstuttet)

fun Yrkesaktivitet.tidsstuttet() = copy(opprettet = opprettet.truncatedTo(MILLIS))
