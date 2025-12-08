package no.nav.helse.bakrommet.kafka

import no.nav.helse.bakrommet.behandling.BehandlingStatus
import no.nav.helse.bakrommet.behandling.dagoversikt.Dagtype
import no.nav.helse.bakrommet.behandling.dagoversikt.Kilde
import no.nav.helse.bakrommet.kafka.dto.saksbehandlingsperiode.DagtypeKafkaDto
import no.nav.helse.bakrommet.kafka.dto.saksbehandlingsperiode.KildeKafkaDto
import no.nav.helse.bakrommet.kafka.dto.saksbehandlingsperiode.SaksbehandlingsperiodeStatusKafkaDto

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
        Dagtype.Behandlingsdag -> DagtypeKafkaDto.Behandlingsdag
    }

fun Kilde?.tilKafkaDto(): KildeKafkaDto? =
    when (this) {
        Kilde.Søknad -> KildeKafkaDto.Søknad
        Kilde.Saksbehandler -> KildeKafkaDto.Saksbehandler
        null -> null
    }

fun BehandlingStatus.tilKafkaDto(): SaksbehandlingsperiodeStatusKafkaDto =
    when (this) {
        BehandlingStatus.UNDER_BEHANDLING -> SaksbehandlingsperiodeStatusKafkaDto.UNDER_BEHANDLING
        BehandlingStatus.TIL_BESLUTNING -> SaksbehandlingsperiodeStatusKafkaDto.TIL_BESLUTNING
        BehandlingStatus.UNDER_BESLUTNING -> SaksbehandlingsperiodeStatusKafkaDto.UNDER_BESLUTNING
        BehandlingStatus.GODKJENT -> SaksbehandlingsperiodeStatusKafkaDto.GODKJENT
        BehandlingStatus.REVURDERT -> SaksbehandlingsperiodeStatusKafkaDto.REVURDERT
    }
