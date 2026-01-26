package no.nav.helse.bakrommet.e2e.testutils

import no.nav.helse.bakrommet.api.dto.behandling.BehandlingDto
import no.nav.helse.bakrommet.behandling.dokumenter.Dokument
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.YrkesaktivitetDbRecord
import java.time.temporal.ChronoUnit

fun BehandlingDto.truncateTidspunkt() = copy(opprettet = opprettet.truncatedTo(ChronoUnit.MICROS))

@JvmName("tidsstuttetSaksbehandlingsperiode")
fun Iterable<BehandlingDto>.tidsstuttet() = map(BehandlingDto::truncateTidspunkt)

@JvmName("tidsstuttetDokument")
fun Iterable<Dokument>.tidsstuttet() = map(Dokument::tidsstuttet)

fun Dokument.tidsstuttet() = copy(opprettet = opprettet.truncatedTo(ChronoUnit.MICROS))

@JvmName("tidsstuttetYrkesaktivitet")
fun Iterable<YrkesaktivitetDbRecord>.tidsstuttet() = map(YrkesaktivitetDbRecord::tidsstuttet)

fun YrkesaktivitetDbRecord.tidsstuttet() = copy(opprettet = opprettet.truncatedTo(ChronoUnit.MICROS))
