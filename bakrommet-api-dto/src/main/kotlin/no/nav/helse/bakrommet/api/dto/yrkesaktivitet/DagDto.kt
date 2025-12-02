package no.nav.helse.bakrommet.api.dto.yrkesaktivitet

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDate

data class DagDto(
    val dato: LocalDate,
    val dagtype: DagtypeDto,
    val grad: Int?,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val avslåttBegrunnelse: List<String>? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val andreYtelserBegrunnelse: List<String>? = null,
    val kilde: KildeDto?,
)

enum class DagtypeDto {
    Syk,
    SykNav,
    Arbeidsdag,
    Ferie,
    Permisjon,
    Avslått,
    AndreYtelser,
}

enum class KildeDto {
    Søknad,
    Saksbehandler,
}
