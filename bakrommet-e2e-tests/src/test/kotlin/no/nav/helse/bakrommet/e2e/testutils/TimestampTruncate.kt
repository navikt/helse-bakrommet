package no.nav.helse.bakrommet.e2e.testutils

import no.nav.helse.bakrommet.api.dto.behandling.BehandlingDto
import no.nav.helse.bakrommet.behandling.dokumenter.Dokument
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetDbRecord
import java.time.temporal.ChronoUnit.MILLIS
import java.time.temporal.ChronoUnit.SECONDS
import kotlin.collections.map

fun BehandlingDto.truncateTidspunkt() = copy(opprettet = opprettet.truncatedTo(SECONDS))

@JvmName("tidsstuttetSaksbehandlingsperiode")
fun Iterable<BehandlingDto>.tidsstuttet() = map(BehandlingDto::truncateTidspunkt)

@JvmName("tidsstuttetDokument")
fun Iterable<Dokument>.tidsstuttet() = map(Dokument::tidsstuttet)

fun Dokument.tidsstuttet() = copy(opprettet = opprettet.truncatedTo(MILLIS))

@JvmName("tidsstuttetYrkesaktivitet")
fun Iterable<YrkesaktivitetDbRecord>.tidsstuttet() = map(YrkesaktivitetDbRecord::tidsstuttet)

fun YrkesaktivitetDbRecord.tidsstuttet() = copy(opprettet = opprettet.truncatedTo(MILLIS))
