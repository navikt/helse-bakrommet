package no.nav.helse.dto.deserialisering

import no.nav.helse.dto.InntektbeløpDto
import java.time.Year
import java.util.UUID

data class SelvstendigFaktaavklartInntektInnDto(
    val id: UUID,
    val inntektsdata: InntektsdataInnDto,
    val pensjonsgivendeInntekter: List<PensjonsgivendeInntektDto>,
    val anvendtGrunnbeløp: InntektbeløpDto.Årlig,
) {
    data class PensjonsgivendeInntektDto(val årstall: Year, val beløp: InntektbeløpDto.Årlig)
}
