package no.nav.helse.bakrommet.kafka.dto

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDate

/**
 * Enum for dagtyper i Kafka DTOs.
 * Dette er en stabil kontrakt for Kafka-meldinger og bør ikke endres uten versjonering.
 */
enum class DagtypeKafkaDto {
    Syk,
    SykNav,
    Arbeidsdag,
    Ferie,
    Permisjon,
    Avslått,
    AndreYtelser,
}

/**
 * Enum for kilder i Kafka DTOs.
 * Dette er en stabil kontrakt for Kafka-meldinger og bør ikke endres uten versjonering.
 */
enum class KildeKafkaDto {
    Søknad,
    Saksbehandler,
}

/**
 * Enum for status på saksbehandlingsperiode i Kafka DTOs.
 * Dette er en stabil kontrakt for Kafka-meldinger og bør ikke endres uten versjonering.
 */
enum class SaksbehandlingsperiodeStatusKafkaDto {
    UNDER_BEHANDLING,
    TIL_BESLUTNING,
    UNDER_BESLUTNING,
    GODKJENT,
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class DagKafkaDto(
    val dato: LocalDate,
    val dagtype: DagtypeKafkaDto,
    val refusjonØre: Int?,
    val utbetalingØre: Int?,
    val grad: Int?,
    val totalGrad: Int?,
    val avslåttBegrunnelse: List<String>? = null,
    val andreYtelserBegrunnelse: List<String>? = null,
    val kilde: KildeKafkaDto?,
)
