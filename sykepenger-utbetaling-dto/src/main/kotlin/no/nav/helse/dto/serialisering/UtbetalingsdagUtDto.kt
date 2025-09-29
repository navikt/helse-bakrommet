package no.nav.helse.dto.serialisering

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
    JsonSubTypes.Type(value = UtbetalingsdagUtDto.ArbeidsgiverperiodeDagDto::class, name = "ArbeidsgiverperiodeDagDto"),
    JsonSubTypes.Type(value = UtbetalingsdagUtDto.ArbeidsgiverperiodeDagNavDto::class, name = "ArbeidsgiverperiodeDagNavDto"),
    JsonSubTypes.Type(value = UtbetalingsdagUtDto.NavDagDto::class, name = "NavDagDto"),
    JsonSubTypes.Type(value = UtbetalingsdagUtDto.NavHelgDagDto::class, name = "NavHelgDagDto"),
    JsonSubTypes.Type(value = UtbetalingsdagUtDto.FridagDto::class, name = "FridagDto"),
    JsonSubTypes.Type(value = UtbetalingsdagUtDto.ArbeidsdagDto::class, name = "ArbeidsdagDto"),
    JsonSubTypes.Type(value = UtbetalingsdagUtDto.AvvistDagDto::class, name = "AvvistDagDto"),
    JsonSubTypes.Type(value = UtbetalingsdagUtDto.ForeldetDagDto::class, name = "ForeldetDagDto"),
    JsonSubTypes.Type(value = UtbetalingsdagUtDto.UkjentDagDto::class, name = "UkjentDagDto"),
    JsonSubTypes.Type(value = UtbetalingsdagUtDto.VentetidsdagDto::class, name = "VentetidsdagDto"),
)
sealed class UtbetalingsdagUtDto {
    abstract val dato: LocalDate
    abstract val økonomi: ØkonomiUtDto

    data class ArbeidsgiverperiodeDagDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiUtDto,
    ) : UtbetalingsdagUtDto()

    data class ArbeidsgiverperiodeDagNavDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiUtDto,
    ) : UtbetalingsdagUtDto()

    data class NavDagDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiUtDto,
    ) : UtbetalingsdagUtDto()

    data class NavHelgDagDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiUtDto,
    ) : UtbetalingsdagUtDto()

    data class FridagDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiUtDto,
    ) : UtbetalingsdagUtDto()

    data class ArbeidsdagDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiUtDto,
    ) : UtbetalingsdagUtDto()

    data class AvvistDagDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiUtDto,
        val begrunnelser: List<BegrunnelseDto>,
    ) : UtbetalingsdagUtDto()

    data class ForeldetDagDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiUtDto,
    ) : UtbetalingsdagUtDto()

    data class UkjentDagDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiUtDto,
    ) : UtbetalingsdagUtDto()

    data class VentetidsdagDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiUtDto,
    ) : UtbetalingsdagUtDto()
}
