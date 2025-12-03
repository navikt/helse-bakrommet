package no.nav.helse.bakrommet.api.dto.utbetalingsberegning

import java.util.UUID

data class YrkesaktivitetUtbetalingsberegningDto(
    val yrkesaktivitetId: UUID,
    val utbetalingstidslinje: UtbetalingstidslinjeDto,
    val dekningsgrad: SporbarDto<ProsentdelDto>?,
)

data class SporbarDto<T>(
    val verdi: T,
    val sporing: BeregningskoderDekningsgradDto,
)
