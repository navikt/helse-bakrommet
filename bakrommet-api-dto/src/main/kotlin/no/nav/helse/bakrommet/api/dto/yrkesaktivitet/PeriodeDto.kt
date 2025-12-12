package no.nav.helse.bakrommet.api.dto.yrkesaktivitet

import java.time.LocalDate

data class PeriodeDto(
    val fom: LocalDate,
    val tom: LocalDate,
)
