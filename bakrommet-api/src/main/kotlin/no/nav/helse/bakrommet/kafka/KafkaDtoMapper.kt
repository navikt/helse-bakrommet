package no.nav.helse.bakrommet.kafka

import no.nav.helse.bakrommet.kafka.dto.saksbehandlingsperiode.DagtypeKafkaDto
import no.nav.helse.bakrommet.kafka.dto.saksbehandlingsperiode.KildeKafkaDto
import no.nav.helse.bakrommet.kafka.dto.saksbehandlingsperiode.SaksbehandlingsperiodeStatusKafkaDto
import no.nav.helse.bakrommet.saksbehandlingsperiode.SaksbehandlingsperiodeStatus
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Dagtype
import no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt.Kilde

/**
 * Mapper-funksjoner for å konvertere interne domeneobjekter til Kafka DTOs.
 * Dette sikrer at Kafka-kontrakten er uavhengig av interne implementasjonsdetaljer.
 */
fun Dagtype.tilKafkaDto(): DagtypeKafkaDto =
    when (this) {
        Dagtype.Syk -> DagtypeKafkaDto.Syk
        Dagtype.SykNav -> DagtypeKafkaDto.SykNav
        Dagtype.Arbeidsdag -> DagtypeKafkaDto.Arbeidsdag
        Dagtype.Ferie -> DagtypeKafkaDto.Ferie
        Dagtype.Permisjon -> DagtypeKafkaDto.Permisjon
        Dagtype.Avslått -> DagtypeKafkaDto.Avslått
        Dagtype.AndreYtelser -> DagtypeKafkaDto.AndreYtelser
    }

fun Kilde?.tilKafkaDto(): KildeKafkaDto? =
    when (this) {
        Kilde.Søknad -> KildeKafkaDto.Søknad
        Kilde.Saksbehandler -> KildeKafkaDto.Saksbehandler
        null -> null
    }

fun SaksbehandlingsperiodeStatus.tilKafkaDto(): SaksbehandlingsperiodeStatusKafkaDto =
    when (this) {
        SaksbehandlingsperiodeStatus.UNDER_BEHANDLING -> SaksbehandlingsperiodeStatusKafkaDto.UNDER_BEHANDLING
        SaksbehandlingsperiodeStatus.TIL_BESLUTNING -> SaksbehandlingsperiodeStatusKafkaDto.TIL_BESLUTNING
        SaksbehandlingsperiodeStatus.UNDER_BESLUTNING -> SaksbehandlingsperiodeStatusKafkaDto.UNDER_BESLUTNING
        SaksbehandlingsperiodeStatus.GODKJENT -> SaksbehandlingsperiodeStatusKafkaDto.GODKJENT
        SaksbehandlingsperiodeStatus.REVURDERT -> SaksbehandlingsperiodeStatusKafkaDto.REVURDERT
    }
