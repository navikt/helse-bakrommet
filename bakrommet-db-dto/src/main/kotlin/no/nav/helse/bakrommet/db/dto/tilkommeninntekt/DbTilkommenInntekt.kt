package no.nav.helse.bakrommet.db.dto.tilkommeninntekt

import java.math.BigDecimal
import java.time.LocalDate

enum class DbTilkommenInntektYrkesaktivitetType {
    VIRKSOMHET,
    PRIVATPERSON,
    NÆRINGSDRIVENDE,
}

data class DbTilkommenInntekt(
    val ident: String,
    val yrkesaktivitetType: DbTilkommenInntektYrkesaktivitetType,
    val fom: LocalDate,
    val tom: LocalDate,
    val inntektForPerioden: BigDecimal,
    val notatTilBeslutter: String,
    val ekskluderteDager: List<LocalDate>,
)
