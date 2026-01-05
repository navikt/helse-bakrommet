package no.nav.helse.bakrommet.api.dto.yrkesaktivitet

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

// TypeArbeidstaker
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = TypeArbeidstakerDto.Ordinær::class, name = "ORDINÆR"),
    JsonSubTypes.Type(value = TypeArbeidstakerDto.Maritim::class, name = "MARITIM"),
    JsonSubTypes.Type(value = TypeArbeidstakerDto.Fisker::class, name = "FISKER"),
    JsonSubTypes.Type(value = TypeArbeidstakerDto.DimmitertVernepliktig::class, name = "DIMMITERT_VERNEPLIKTIG"),
    JsonSubTypes.Type(value = TypeArbeidstakerDto.PrivatArbeidsgiver::class, name = "PRIVAT_ARBEIDSGIVER"),
)
sealed class TypeArbeidstakerDto {
    data class Ordinær(
        val orgnummer: String,
    ) : TypeArbeidstakerDto()

    data class Maritim(
        val orgnummer: String,
    ) : TypeArbeidstakerDto()

    data class Fisker(
        val orgnummer: String,
    ) : TypeArbeidstakerDto()

    data class DimmitertVernepliktig(
        val tjenesteMerEnn28Dager: Boolean,
    ) : TypeArbeidstakerDto()

    data class PrivatArbeidsgiver(
        val arbeidsgiverFnr: String,
    ) : TypeArbeidstakerDto()
}

enum class FrilanserForsikringDto {
    FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG,
    INGEN_FORSIKRING,
}

enum class SelvstendigForsikringDto {
    FORSIKRING_80_PROSENT_FRA_FØRSTE_SYKEDAG,
    FORSIKRING_100_PROSENT_FRA_17_SYKEDAG,
    FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG,
    INGEN_FORSIKRING,
}

// TypeSelvstendigNæringsdrivende
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = TypeSelvstendigNæringsdrivendeDto.Ordinær::class, name = "ORDINÆR"),
    JsonSubTypes.Type(value = TypeSelvstendigNæringsdrivendeDto.BarnepasserEgetHjem::class, name = "BARNEPASSER_EGET_HJEM"),
    JsonSubTypes.Type(value = TypeSelvstendigNæringsdrivendeDto.Fisker::class, name = "FISKER"),
    JsonSubTypes.Type(value = TypeSelvstendigNæringsdrivendeDto.Jordbruker::class, name = "JORDBRUKER"),
    JsonSubTypes.Type(value = TypeSelvstendigNæringsdrivendeDto.Reindrift::class, name = "REINDRIFT"),
)
sealed class TypeSelvstendigNæringsdrivendeDto {
    data class Ordinær(
        val forsikring: SelvstendigForsikringDto,
    ) : TypeSelvstendigNæringsdrivendeDto()

    data class BarnepasserEgetHjem(
        val forsikring: SelvstendigForsikringDto,
    ) : TypeSelvstendigNæringsdrivendeDto()

    data class Fisker(
        val forsikring: FrilanserForsikringDto, // Alltid FORSIKRING_100_PROSENT_FRA_FØRSTE_SYKEDAG
    ) : TypeSelvstendigNæringsdrivendeDto()

    data class Jordbruker(
        val forsikring: SelvstendigForsikringDto,
    ) : TypeSelvstendigNæringsdrivendeDto()

    data class Reindrift(
        val forsikring: SelvstendigForsikringDto,
    ) : TypeSelvstendigNæringsdrivendeDto()
}

// YrkesaktivitetKategorisering
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "inntektskategori")
@JsonSubTypes(
    JsonSubTypes.Type(value = YrkesaktivitetKategoriseringDto.Arbeidstaker::class, name = "ARBEIDSTAKER"),
    JsonSubTypes.Type(value = YrkesaktivitetKategoriseringDto.Frilanser::class, name = "FRILANSER"),
    JsonSubTypes.Type(value = YrkesaktivitetKategoriseringDto.SelvstendigNæringsdrivende::class, name = "SELVSTENDIG_NÆRINGSDRIVENDE"),
    JsonSubTypes.Type(value = YrkesaktivitetKategoriseringDto.Inaktiv::class, name = "INAKTIV"),
    JsonSubTypes.Type(value = YrkesaktivitetKategoriseringDto.Arbeidsledig::class, name = "ARBEIDSLEDIG"),
)
sealed class YrkesaktivitetKategoriseringDto {
    data class Arbeidstaker(
        val sykmeldt: Boolean,
        val typeArbeidstaker: TypeArbeidstakerDto,
    ) : YrkesaktivitetKategoriseringDto()

    data class Frilanser(
        val sykmeldt: Boolean,
        val orgnummer: String,
        val forsikring: FrilanserForsikringDto,
    ) : YrkesaktivitetKategoriseringDto()

    data class SelvstendigNæringsdrivende(
        val sykmeldt: Boolean,
        val typeSelvstendigNæringsdrivende: TypeSelvstendigNæringsdrivendeDto,
    ) : YrkesaktivitetKategoriseringDto()

    data class Inaktiv(
        val sykmeldt: Boolean = true,
    ) : YrkesaktivitetKategoriseringDto()

    data class Arbeidsledig(
        val sykmeldt: Boolean = true,
    ) : YrkesaktivitetKategoriseringDto()
}

fun YrkesaktivitetKategoriseringDto.maybeOrgnummer(): String? =
    when (this) {
        is YrkesaktivitetKategoriseringDto.Arbeidstaker ->
            when (val type = this.typeArbeidstaker) {
                is TypeArbeidstakerDto.Ordinær -> type.orgnummer
                is TypeArbeidstakerDto.Maritim -> type.orgnummer
                is TypeArbeidstakerDto.Fisker -> type.orgnummer
                else -> null
            }

        is YrkesaktivitetKategoriseringDto.Frilanser -> this.orgnummer
        else -> null
    }
