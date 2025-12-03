package no.nav.helse.bakrommet.api.dto.utbetalingsberegning

data class BeregningDataDto(
    val yrkesaktiviteter: List<YrkesaktivitetUtbetalingsberegningDto>,
    val spilleromOppdrag: SpilleromOppdragDto,
)
