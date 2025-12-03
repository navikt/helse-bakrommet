package no.nav.helse.bakrommet.api.dto.sykepengegrunnlag

import java.time.LocalDate

data class OpprettSykepengegrunnlagRequestDto(
    val beregningsgrunnlag: Double, // BigDecimal som Double
    val begrunnelse: String,
    val datoForGBegrensning: LocalDate? = null,
    val beregningskoder: List<BeregningskoderSykepengegrunnlagDto>,
)
