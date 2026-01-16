package no.nav.helse.bakrommet.db.dto.yrkesaktivitet

import java.time.LocalDate

data class DbPeriode(
    val fom: LocalDate,
    val tom: LocalDate,
)
