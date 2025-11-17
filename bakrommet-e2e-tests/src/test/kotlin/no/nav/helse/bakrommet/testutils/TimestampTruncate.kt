package no.nav.helse.bakrommet.testutils

import no.nav.helse.bakrommet.behandling.Saksbehandlingsperiode
import no.nav.helse.bakrommet.behandling.dokumenter.Dokument
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetDbRecord
import java.time.temporal.ChronoUnit.MILLIS
import java.time.temporal.ChronoUnit.SECONDS
import kotlin.collections.map

fun Saksbehandlingsperiode.truncateTidspunkt() = copy(opprettet = opprettet.truncatedTo(SECONDS))

@JvmName("tidsstuttetSaksbehandlingsperiode")
fun Iterable<Saksbehandlingsperiode>.tidsstuttet() = map(Saksbehandlingsperiode::truncateTidspunkt)

@JvmName("tidsstuttetDokument")
fun Iterable<Dokument>.tidsstuttet() = map(Dokument::tidsstuttet)

fun Dokument.tidsstuttet() = copy(opprettet = opprettet.truncatedTo(MILLIS))

@JvmName("tidsstuttetYrkesaktivitet")
fun Iterable<YrkesaktivitetDbRecord>.tidsstuttet() = map(YrkesaktivitetDbRecord::tidsstuttet)

fun YrkesaktivitetDbRecord.tidsstuttet() = copy(opprettet = opprettet.truncatedTo(MILLIS))
