package no.nav.helse.bakrommet.db.dto.yrkesaktivitet

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDate

data class DbDag(
    val dato: LocalDate,
    val dagtype: DbDagtype,
    val grad: Int?,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val avslåttBegrunnelse: List<String>? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val andreYtelserBegrunnelse: List<String>? = null,
    val kilde: DbKilde?,
)

enum class DbDagtype {
    Syk,
    SykNav,
    Arbeidsdag,
    Ferie,
    Permisjon,
    Avslått,
    AndreYtelser,
    Behandlingsdag,
}

enum class DbKilde {
    Søknad,
    Saksbehandler,
}
