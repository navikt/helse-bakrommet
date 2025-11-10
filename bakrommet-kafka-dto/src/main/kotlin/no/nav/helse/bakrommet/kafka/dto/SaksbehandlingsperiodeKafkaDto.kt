package no.nav.helse.bakrommet.kafka.dto

import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

data class SaksbehandlingsperiodeKafkaDto(
    val id: UUID,
    val spilleromPersonId: String,
    val fnr: String,
    val opprettet: OffsetDateTime,
    val opprettetAvNavIdent: String,
    val opprettetAvNavn: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val status: SaksbehandlingsperiodeStatusKafkaDto,
    val beslutterNavIdent: String?,
    val skjæringstidspunkt: LocalDate?,
    val yrkesaktiviteter: List<YrkesaktivitetKafkaDto>,
)
