package no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.helse.dto.InntektbeløpDto
import java.time.LocalDate

// ARBEIDSTAKER
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = ArbeidstakerInntektRequest.Inntektsmelding::class, name = "INNTEKTSMELDING"),
    JsonSubTypes.Type(value = ArbeidstakerInntektRequest.Ainntekt::class, name = "AINNTEKT"),
    JsonSubTypes.Type(value = ArbeidstakerInntektRequest.Skjønnsfastsatt::class, name = "SKJONNSFASTSETTELSE"),
    JsonSubTypes.Type(value = ArbeidstakerInntektRequest.ManueltBeregnet::class, name = "MANUELT_BEREGNET"),
)
sealed class ArbeidstakerInntektRequest {
    abstract val begrunnelse: String?

    data class Inntektsmelding(
        val inntektsmeldingId: String,
        override val begrunnelse: String,
    ) : ArbeidstakerInntektRequest()

    class Ainntekt(
        override val begrunnelse: String,
    ) : ArbeidstakerInntektRequest()

    data class Skjønnsfastsatt(
        val månedsbeløp: InntektbeløpDto.MånedligDouble,
        val årsak: ArbeidstakerSkjønnsfastsettelseÅrsak,
        override val begrunnelse: String,
        val refusjon: RefusjonInfo? = null,
    ) : ArbeidstakerInntektRequest()

    data class ManueltBeregnet(
        val månedsbeløp: InntektbeløpDto.MånedligDouble,
        override val begrunnelse: String,
    ) : ArbeidstakerInntektRequest()
}

enum class ArbeidstakerSkjønnsfastsettelseÅrsak {
    AVVIK_25_PROSENT,
    MANGELFULL_RAPPORTERING,
    TIDSAVGRENSET,
}

// SELVSTENDIG_NÆRINGSDRIVENDE / INAKTIV
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(
        value = PensjonsgivendeInntektRequest.PensjonsgivendeInntekt::class,
        name = "PENSJONSGIVENDE_INNTEKT",
    ),
    JsonSubTypes.Type(value = PensjonsgivendeInntektRequest.Skjønnsfastsatt::class, name = "SKJONNSFASTSETTELSE"),
)
sealed class PensjonsgivendeInntektRequest {
    abstract val begrunnelse: String?

    class PensjonsgivendeInntekt(
        override val begrunnelse: String,
    ) : PensjonsgivendeInntektRequest()

    data class Skjønnsfastsatt(
        val årsinntekt: InntektbeløpDto.Årlig,
        val årsak: PensjonsgivendeSkjønnsfastsettelseÅrsak,
        override val begrunnelse: String,
    ) : PensjonsgivendeInntektRequest()
}

enum class PensjonsgivendeSkjønnsfastsettelseÅrsak {
    AVVIK_25_PROSENT_VARIG_ENDRING,
    SISTE_TRE_YRKESAKTIV,
}

// FRILANSER
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = FrilanserInntektRequest.Ainntekt::class, name = "AINNTEKT"),
    JsonSubTypes.Type(value = FrilanserInntektRequest.Skjønnsfastsatt::class, name = "SKJONNSFASTSETTELSE"),
)
sealed class FrilanserInntektRequest {
    abstract val begrunnelse: String?

    class Ainntekt(
        override val begrunnelse: String,
    ) : FrilanserInntektRequest()

    data class Skjønnsfastsatt(
        val månedsbeløp: InntektbeløpDto.MånedligDouble,
        val årsak: FrilanserSkjønnsfastsettelseÅrsak,
        override val begrunnelse: String,
    ) : FrilanserInntektRequest()
}

enum class FrilanserSkjønnsfastsettelseÅrsak {
    AVVIK_25_PROSENT,
    MANGELFULL_RAPPORTERING,
}

// ARBEIDSLEDIG
data class ArbeidsledigInntektRequest(
    val begrunnelse: String?,
    val type: ArbeidsledigInntektType,
    val månedligBeløp: InntektbeløpDto.MånedligDouble,
)

enum class ArbeidsledigInntektType {
    DAGPENGER,
    VENTELONN,
    VARTPENGER,
}

// Union av alle requests med Jackson discriminator
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "inntektskategori")
@JsonSubTypes(
    JsonSubTypes.Type(value = InntektRequest.Arbeidstaker::class, name = "ARBEIDSTAKER"),
    JsonSubTypes.Type(value = InntektRequest.SelvstendigNæringsdrivende::class, name = "SELVSTENDIG_NÆRINGSDRIVENDE"),
    JsonSubTypes.Type(value = InntektRequest.Inaktiv::class, name = "INAKTIV"),
    JsonSubTypes.Type(value = InntektRequest.Frilanser::class, name = "FRILANSER"),
    JsonSubTypes.Type(value = InntektRequest.Arbeidsledig::class, name = "ARBEIDSLEDIG"),
)
sealed class InntektRequest {
    data class Arbeidstaker(
        val data: ArbeidstakerInntektRequest,
    ) : InntektRequest()

    data class SelvstendigNæringsdrivende(
        val data: PensjonsgivendeInntektRequest,
    ) : InntektRequest()

    data class Inaktiv(
        val data: PensjonsgivendeInntektRequest,
    ) : InntektRequest()

    data class Frilanser(
        val data: FrilanserInntektRequest,
    ) : InntektRequest()

    data class Arbeidsledig(
        val data: ArbeidsledigInntektRequest,
    ) : InntektRequest()
}

// Hjelpeklasser
data class RefusjonInfo(
    val fra: LocalDate,
    val til: LocalDate,
    val beløp: Int,
)
