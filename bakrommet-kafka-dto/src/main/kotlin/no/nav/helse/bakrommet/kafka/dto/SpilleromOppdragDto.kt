package no.nav.helse.bakrommet.kafka.dto

import java.time.LocalDate

data class SpilleromOppdragDto(
    val spilleromUtbetalingId: String,
    val oppdragDto: List<OppdragDto>,
)

data class OppdragDto(
    val mottaker: String,
    val fagområde: String,
    val linjer: List<UtbetalingslinjeDto>,
)

data class UtbetalingslinjeDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val beløp: Int,
    val grad: Int,
    val klassekode: String,
)
