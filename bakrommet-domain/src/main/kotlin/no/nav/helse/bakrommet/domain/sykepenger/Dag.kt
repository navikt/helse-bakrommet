package no.nav.helse.bakrommet.domain.sykepenger

import java.time.LocalDate

data class Dag(
    val dato: LocalDate,
    val dagtype: Dagtype,
    val grad: Int?,
    val avslåttBegrunnelse: List<String>? = null,
    val andreYtelserBegrunnelse: List<String>? = null,
    val kilde: Kilde?,
)

enum class Dagtype {
    Syk,
    SykNav,
    Arbeidsdag,
    Ferie,
    Permisjon,
    Avslått,
    AndreYtelser,
    Behandlingsdag,
}

enum class Kilde {
    Søknad,
    Saksbehandler,
}
