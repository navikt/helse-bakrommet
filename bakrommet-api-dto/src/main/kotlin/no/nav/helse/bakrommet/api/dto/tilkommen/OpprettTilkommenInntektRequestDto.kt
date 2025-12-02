package no.nav.helse.bakrommet.api.dto.tilkommen

import no.nav.helse.bakrommet.api.dto.tidslinje.TilkommenInntektYrkesaktivitetType
import java.math.BigDecimal
import java.time.LocalDate

data class OpprettTilkommenInntektRequestDto(
    val ident: String,
    val yrkesaktivitetType: TilkommenInntektYrkesaktivitetType,
    val fom: LocalDate,
    val tom: LocalDate,
    val inntektForPerioden: BigDecimal,
    val notatTilBeslutter: String,
    val ekskluderteDager: List<LocalDate>,
)
