package no.nav.helse.bakrommet.db.dto.yrkesaktivitet

import java.time.Year

data class DbInntektÅr(
    val år: Year,
    val rapportertinntekt: Double,
    val justertÅrsgrunnlag: Double,
    val antallGKompensert: Double,
    val snittG: Double,
)
