package no.nav.helse.bakrommet.db.dto.yrkesaktivitet

import java.time.Year

data class DbInntektÅr(
    val år: Year,
    val rapportertinntekt: DbInntekt.Årlig,
    val justertÅrsgrunnlag: DbInntekt.Årlig,
    val antallGKompensert: Double,
    val snittG: DbInntekt.Årlig,
)
