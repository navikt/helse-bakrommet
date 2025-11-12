package no.nav.helse.bakrommet.kafka.dto.oppdrag

import java.time.LocalDate

data class SpilleromOppdragDto(
    val spilleromUtbetalingId: String,
    val oppdrag: List<OppdragDto>,
    val maksdato: LocalDate? = null,
)

data class OppdragDto(
    val mottaker: String,
    val fagområde: String,
    val linjer: List<UtbetalingslinjeDto>,
    val totalbeløp: Int,
)

data class UtbetalingslinjeDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val beløp: Int,
    val grad: Int,
    val klassekode: String,
    val stønadsdager: Int,
)
