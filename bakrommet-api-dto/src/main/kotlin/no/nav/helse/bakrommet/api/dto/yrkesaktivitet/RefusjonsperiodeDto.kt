package no.nav.helse.bakrommet.api.dto.yrkesaktivitet

import java.time.LocalDate

data class RefusjonsperiodeDto(
    val fom: LocalDate,
    val tom: LocalDate?,
    val beløp: Double,
)
