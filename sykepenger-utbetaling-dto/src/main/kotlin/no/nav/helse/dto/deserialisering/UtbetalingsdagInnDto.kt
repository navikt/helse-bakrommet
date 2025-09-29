package no.nav.helse.dto.deserialisering

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.helse.dto.BegrunnelseDto
import java.time.LocalDate

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "@type",
)
@JsonSubTypes(
    JsonSubTypes.Type(value = UtbetalingsdagInnDto.ArbeidsgiverperiodeDagDto::class, name = "ArbeidsgiverperiodeDagDto"),
    JsonSubTypes.Type(value = UtbetalingsdagInnDto.ArbeidsgiverperiodeDagNavDto::class, name = "ArbeidsgiverperiodeDagNavDto"),
    JsonSubTypes.Type(value = UtbetalingsdagInnDto.NavDagDto::class, name = "NavDagDto"),
    JsonSubTypes.Type(value = UtbetalingsdagInnDto.NavHelgDagDto::class, name = "NavHelgDagDto"),
    JsonSubTypes.Type(value = UtbetalingsdagInnDto.FridagDto::class, name = "FridagDto"),
    JsonSubTypes.Type(value = UtbetalingsdagInnDto.ArbeidsdagDto::class, name = "ArbeidsdagDto"),
    JsonSubTypes.Type(value = UtbetalingsdagInnDto.AvvistDagDto::class, name = "AvvistDagDto"),
    JsonSubTypes.Type(value = UtbetalingsdagInnDto.ForeldetDagDto::class, name = "ForeldetDagDto"),
    JsonSubTypes.Type(value = UtbetalingsdagInnDto.UkjentDagDto::class, name = "UkjentDagDto"),
    JsonSubTypes.Type(value = UtbetalingsdagInnDto.VentetidsdagDto::class, name = "VentetidsdagDto"),
)
sealed class UtbetalingsdagInnDto {
    abstract val dato: LocalDate
    abstract val økonomi: ØkonomiInnDto

    data class ArbeidsgiverperiodeDagDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiInnDto,
    ) : UtbetalingsdagInnDto()

    data class ArbeidsgiverperiodeDagNavDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiInnDto,
    ) : UtbetalingsdagInnDto()

    data class NavDagDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiInnDto,
    ) : UtbetalingsdagInnDto()

    data class NavHelgDagDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiInnDto,
    ) : UtbetalingsdagInnDto()

    data class FridagDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiInnDto,
    ) : UtbetalingsdagInnDto()

    data class ArbeidsdagDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiInnDto,
    ) : UtbetalingsdagInnDto()

    data class AvvistDagDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiInnDto,
        val begrunnelser: List<BegrunnelseDto>,
    ) : UtbetalingsdagInnDto()

    data class ForeldetDagDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiInnDto,
    ) : UtbetalingsdagInnDto()

    data class UkjentDagDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiInnDto,
    ) : UtbetalingsdagInnDto()

    data class VentetidsdagDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiInnDto,
    ) : UtbetalingsdagInnDto()
}
