package no.nav.helse.bakrommet.api.dto.yrkesaktivitet

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

// ARBEIDSTAKER
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = ArbeidstakerInntektRequestDto.Inntektsmelding::class, name = "INNTEKTSMELDING"),
    JsonSubTypes.Type(value = ArbeidstakerInntektRequestDto.Ainntekt::class, name = "AINNTEKT"),
    JsonSubTypes.Type(value = ArbeidstakerInntektRequestDto.Skjønnsfastsatt::class, name = "SKJONNSFASTSETTELSE"),
)
@JsonInclude(JsonInclude.Include.NON_NULL)
sealed class ArbeidstakerInntektRequestDto {
    abstract val begrunnelse: String?
    abstract val refusjon: List<RefusjonsperiodeDto>?

    data class Inntektsmelding(
        val inntektsmeldingId: String,
        override val begrunnelse: String? = null,
        override val refusjon: List<RefusjonsperiodeDto>? = null,
    ) : ArbeidstakerInntektRequestDto()

    data class Ainntekt(
        override val begrunnelse: String,
        override val refusjon: List<RefusjonsperiodeDto>? = null,
    ) : ArbeidstakerInntektRequestDto()

    data class Skjønnsfastsatt(
        val årsinntekt: Double, // Årlig beløp som Double i stedet for InntektbeløpDto.Årlig
        val årsak: ArbeidstakerSkjønnsfastsettelseÅrsakDto,
        override val begrunnelse: String,
        override val refusjon: List<RefusjonsperiodeDto>? = null,
    ) : ArbeidstakerInntektRequestDto()
}

enum class ArbeidstakerSkjønnsfastsettelseÅrsakDto {
    AVVIK_25_PROSENT,
    MANGELFULL_RAPPORTERING,
    TIDSAVGRENSET,
}

// SELVSTENDIG_NÆRINGSDRIVENDE / INAKTIV
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(
        value = PensjonsgivendeInntektRequestDto.PensjonsgivendeInntekt::class,
        name = "PENSJONSGIVENDE_INNTEKT",
    ),
    JsonSubTypes.Type(value = PensjonsgivendeInntektRequestDto.Skjønnsfastsatt::class, name = "SKJONNSFASTSETTELSE"),
)
sealed class PensjonsgivendeInntektRequestDto {
    abstract val begrunnelse: String

    data class PensjonsgivendeInntekt(
        override val begrunnelse: String,
    ) : PensjonsgivendeInntektRequestDto()

    data class Skjønnsfastsatt(
        val årsinntekt: Double, // Årlig beløp som Double i stedet for InntektbeløpDto.Årlig
        val årsak: PensjonsgivendeSkjønnsfastsettelseÅrsakDto,
        override val begrunnelse: String,
    ) : PensjonsgivendeInntektRequestDto()
}

enum class PensjonsgivendeSkjønnsfastsettelseÅrsakDto {
    AVVIK_25_PROSENT_VARIG_ENDRING,
    SISTE_TRE_YRKESAKTIV,
}

// FRILANSER
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = FrilanserInntektRequestDto.Ainntekt::class, name = "AINNTEKT"),
    JsonSubTypes.Type(value = FrilanserInntektRequestDto.Skjønnsfastsatt::class, name = "SKJONNSFASTSETTELSE"),
)
sealed class FrilanserInntektRequestDto {
    abstract val begrunnelse: String

    data class Ainntekt(
        override val begrunnelse: String,
    ) : FrilanserInntektRequestDto()

    data class Skjønnsfastsatt(
        val årsinntekt: Double, // Årlig beløp som Double i stedet for InntektbeløpDto.Årlig
        val årsak: FrilanserSkjønnsfastsettelseÅrsakDto,
        override val begrunnelse: String,
    ) : FrilanserInntektRequestDto()
}

enum class FrilanserSkjønnsfastsettelseÅrsakDto {
    AVVIK_25_PROSENT,
    MANGELFULL_RAPPORTERING,
}

// ARBEIDSLEDIG
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = ArbeidsledigInntektRequestDto.Dagpenger::class, name = "DAGPENGER"),
    JsonSubTypes.Type(value = ArbeidsledigInntektRequestDto.Ventelønn::class, name = "VENTELONN"),
    JsonSubTypes.Type(value = ArbeidsledigInntektRequestDto.Vartpenger::class, name = "VARTPENGER"),
)
sealed class ArbeidsledigInntektRequestDto {
    abstract val begrunnelse: String

    data class Dagpenger(
        val dagbeløp: Int, // Daglig beløp som Int i stedet for InntektbeløpDto.DagligInt
        override val begrunnelse: String,
    ) : ArbeidsledigInntektRequestDto()

    data class Ventelønn(
        val årsinntekt: Double, // Årlig beløp som Double i stedet for InntektbeløpDto.Årlig
        override val begrunnelse: String,
    ) : ArbeidsledigInntektRequestDto()

    data class Vartpenger(
        val årsinntekt: Double, // Årlig beløp som Double i stedet for InntektbeløpDto.Årlig
        override val begrunnelse: String,
    ) : ArbeidsledigInntektRequestDto()
}

// Union av alle requests med Jackson discriminator
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "inntektskategori")
@JsonSubTypes(
    JsonSubTypes.Type(value = InntektRequestDto.Arbeidstaker::class, name = "ARBEIDSTAKER"),
    JsonSubTypes.Type(value = InntektRequestDto.SelvstendigNæringsdrivende::class, name = "SELVSTENDIG_NÆRINGSDRIVENDE"),
    JsonSubTypes.Type(value = InntektRequestDto.Inaktiv::class, name = "INAKTIV"),
    JsonSubTypes.Type(value = InntektRequestDto.Frilanser::class, name = "FRILANSER"),
    JsonSubTypes.Type(value = InntektRequestDto.Arbeidsledig::class, name = "ARBEIDSLEDIG"),
)
sealed class InntektRequestDto {
    data class Arbeidstaker(
        val data: ArbeidstakerInntektRequestDto,
    ) : InntektRequestDto()

    data class SelvstendigNæringsdrivende(
        val data: PensjonsgivendeInntektRequestDto,
    ) : InntektRequestDto()

    data class Inaktiv(
        val data: PensjonsgivendeInntektRequestDto,
    ) : InntektRequestDto()

    data class Frilanser(
        val data: FrilanserInntektRequestDto,
    ) : InntektRequestDto()

    data class Arbeidsledig(
        val data: ArbeidsledigInntektRequestDto,
    ) : InntektRequestDto()
}
