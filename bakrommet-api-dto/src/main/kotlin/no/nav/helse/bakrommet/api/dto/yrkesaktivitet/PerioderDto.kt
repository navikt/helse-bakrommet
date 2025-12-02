package no.nav.helse.bakrommet.api.dto.yrkesaktivitet

data class PerioderDto(
    val type: PeriodetypeDto,
    val perioder: List<PeriodeDto>,
)

enum class PeriodetypeDto {
    ARBEIDSGIVERPERIODE,
    VENTETID,
    VENTETID_INAKTIV,
}
