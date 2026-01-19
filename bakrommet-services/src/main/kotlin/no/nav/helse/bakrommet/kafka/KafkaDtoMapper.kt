package no.nav.helse.bakrommet.kafka

import no.nav.helse.bakrommet.behandling.BehandlingStatus
import no.nav.helse.bakrommet.kafka.dto.saksbehandlingsperiode.SaksbehandlingsperiodeStatusKafkaDto

fun BehandlingStatus.tilKafkaDto(): SaksbehandlingsperiodeStatusKafkaDto =
    when (this) {
        BehandlingStatus.UNDER_BEHANDLING -> SaksbehandlingsperiodeStatusKafkaDto.UNDER_BEHANDLING
        BehandlingStatus.TIL_BESLUTNING -> SaksbehandlingsperiodeStatusKafkaDto.TIL_BESLUTNING
        BehandlingStatus.UNDER_BESLUTNING -> SaksbehandlingsperiodeStatusKafkaDto.UNDER_BESLUTNING
        BehandlingStatus.GODKJENT -> SaksbehandlingsperiodeStatusKafkaDto.GODKJENT
        BehandlingStatus.REVURDERT -> SaksbehandlingsperiodeStatusKafkaDto.REVURDERT
    }
