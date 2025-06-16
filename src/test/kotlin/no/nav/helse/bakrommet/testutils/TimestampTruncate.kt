package no.nav.helse.bakrommet.testutils

import no.nav.helse.bakrommet.saksbehandlingsperiode.Dokument
import no.nav.helse.bakrommet.saksbehandlingsperiode.Saksbehandlingsperiode
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntektsforhold.Inntektsforhold
import java.time.temporal.ChronoUnit.MILLIS
import java.time.temporal.ChronoUnit.SECONDS

fun Saksbehandlingsperiode.truncateTidspunkt() = copy(opprettet = opprettet.truncatedTo(SECONDS))

@JvmName("tidsstuttetDokument")
fun Iterable<Dokument>.tidsstuttet() = map(Dokument::tidsstuttet)

fun Dokument.tidsstuttet() = copy(opprettet = opprettet.truncatedTo(MILLIS))

@JvmName("tidsstuttetInntektsforhold")
fun Iterable<Inntektsforhold>.tidsstuttet() = map(Inntektsforhold::tidsstuttet)

fun Inntektsforhold.tidsstuttet() = copy(opprettet = opprettet.truncatedTo(MILLIS))
