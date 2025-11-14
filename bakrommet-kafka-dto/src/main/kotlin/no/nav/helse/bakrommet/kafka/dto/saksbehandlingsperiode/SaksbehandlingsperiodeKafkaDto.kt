package no.nav.helse.bakrommet.kafka.dto.saksbehandlingsperiode

import no.nav.helse.bakrommet.kafka.dto.oppdrag.SpilleromOppdragDto
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

data class SaksbehandlingsperiodeKafkaDto(
    val id: UUID,
    val fnr: String,
    val opprettet: OffsetDateTime,
    val opprettetAvNavIdent: String,
    val opprettetAvNavn: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val status: SaksbehandlingsperiodeStatusKafkaDto,
    val beslutterNavIdent: String?,
    val skjæringstidspunkt: LocalDate?,
    val yrkesaktiviteter: List<Any>,
    val spilleromOppdrag: SpilleromOppdragDto?,
    val revurdererSaksbehandlingsperiodeId: UUID?,
)
