package no.nav.helse.bakrommet.api.dto.utbetalingsberegning

import java.time.LocalDate

data class UtbetalingstidslinjeDto(
    val dager: List<UtbetalingsdagDto>,
)

data class UtbetalingsdagDto(
    val dato: LocalDate,
    val økonomi: ØkonomiDto,
)

data class ØkonomiDto(
    val grad: Double,
    val totalGrad: Double,
    val utbetalingsgrad: Double,
    val arbeidsgiverbeløp: Double?,
    val personbeløp: Double?,
)
