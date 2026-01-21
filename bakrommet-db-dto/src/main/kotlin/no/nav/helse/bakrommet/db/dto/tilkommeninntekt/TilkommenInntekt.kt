package no.nav.helse.bakrommet.db.dto.tilkommeninntekt

import java.math.BigDecimal
import java.time.LocalDate

enum class TilkommenInntektYrkesaktivitetType {
    VIRKSOMHET,
    PRIVATPERSON,
    NÆRINGSDRIVENDE,
}

data class TilkommenInntekt(
    val ident: String,
    val yrkesaktivitetType: TilkommenInntektYrkesaktivitetType,
    val fom: LocalDate,
    val tom: LocalDate,
    val inntektForPerioden: BigDecimal,
    val notatTilBeslutter: String,
    val ekskluderteDager: List<LocalDate>,
)
