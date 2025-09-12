package no.nav.helse.bakrommet.saksbehandlingsperiode.dagoversikt

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.bakrommet.util.objectMapper
import java.time.LocalDate

data class Dag(
    val dato: LocalDate,
    val dagtype: Dagtype,
    val grad: Int?,
    val avslåttBegrunnelse: List<String>,
    val kilde: Kilde?,
)

enum class Dagtype {
    Syk,
    SykNav,
    Arbeidsdag,
    Helg,
    Ferie,
    Permisjon,
    Avslått,
    AndreYtelser,
    Ventetid,
}

enum class Kilde {
    Søknad,
    Saksbehandler,
}

/**
 * Extension function for å parse dagoversikt fra JsonNode til List<Dag>
 * Håndterer både array-format og returnerer tom liste hvis parsing feiler
 */
fun JsonNode?.tilDagoversikt(): List<Dag> {
    if (this == null) return emptyList()

    return try {
        if (isArray) {
            map { dagJson ->
                objectMapper.treeToValue(dagJson, Dag::class.java)
            }
        } else {
            throw RuntimeException("Forventet array for")
        }
    } catch (e: Exception) {
        throw RuntimeException("feil ved parsing av dagoversikt", e)
    }
}

/**
 * Extension function for å parse dagoversikt fra JsonNode til List<JsonNode>
 * Håndterer både array-format og returnerer tom liste hvis parsing feiler
 */
fun JsonNode?.tilDagoversiktJson(): List<JsonNode> {
    if (this == null) return emptyList()

    return try {
        if (isArray) {
            toList()
        } else {
            emptyList()
        }
    } catch (e: Exception) {
        emptyList()
    }
}
