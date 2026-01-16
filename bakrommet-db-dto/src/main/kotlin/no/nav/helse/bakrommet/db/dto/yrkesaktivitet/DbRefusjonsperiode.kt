package no.nav.helse.bakrommet.db.dto.yrkesaktivitet

import java.time.LocalDate

data class DbRefusjonsperiode(
    val fom: LocalDate,
    val tom: LocalDate?,
    val beløp: DbInntekt.Månedlig,
)
