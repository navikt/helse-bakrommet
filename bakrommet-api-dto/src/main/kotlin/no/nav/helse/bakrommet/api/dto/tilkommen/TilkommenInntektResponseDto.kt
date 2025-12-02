package no.nav.helse.bakrommet.api.dto.tilkommen

import no.nav.helse.bakrommet.api.dto.interfaces.ApiResponse
import no.nav.helse.bakrommet.api.dto.tidslinje.TilkommenInntektYrkesaktivitetType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class TilkommenInntektResponseDto(
    val id: UUID,
    val ident: String,
    val yrkesaktivitetType: TilkommenInntektYrkesaktivitetType,
    val fom: LocalDate,
    val tom: LocalDate,
    val inntektForPerioden: BigDecimal,
    val notatTilBeslutter: String,
    val ekskluderteDager: List<LocalDate>,
    val opprettet: OffsetDateTime,
    val opprettetAvNavIdent: String,
) : ApiResponse
