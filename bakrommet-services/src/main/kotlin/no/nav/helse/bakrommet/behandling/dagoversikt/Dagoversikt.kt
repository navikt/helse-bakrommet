package no.nav.helse.bakrommet.behandling.dagoversikt

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.Dagoversikt
import no.nav.helse.bakrommet.util.objectMapper
import java.time.LocalDate

data class Dag(
    val dato: LocalDate,
    val dagtype: Dagtype,
    val grad: Int?,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val avslåttBegrunnelse: List<String>? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL)
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

/**
 * Extension function for å parse dagoversikt fra JsonNode til List<Dag>
 * Håndterer både array-format og returnerer tom liste hvis parsing feiler
 */
fun String?.tilDagoversikt(): Dagoversikt? {
    if (this == null) return null
    return try {
        objectMapper.readValue(this)
    } catch (e: Exception) {
        throw RuntimeException("feil ved parsing av dagoversikt", e)
    }
}
