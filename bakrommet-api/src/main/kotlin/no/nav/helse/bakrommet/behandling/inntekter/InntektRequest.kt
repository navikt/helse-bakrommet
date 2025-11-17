package no.nav.helse.bakrommet.behandling.inntekter

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.Refusjonsperiode
import no.nav.helse.dto.InntektbeløpDto

// ARBEIDSTAKER
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = ArbeidstakerInntektRequest.Inntektsmelding::class, name = "INNTEKTSMELDING"),
    JsonSubTypes.Type(value = ArbeidstakerInntektRequest.Ainntekt::class, name = "AINNTEKT"),
    JsonSubTypes.Type(value = ArbeidstakerInntektRequest.Skjønnsfastsatt::class, name = "SKJONNSFASTSETTELSE"),
    JsonSubTypes.Type(value = ArbeidstakerInntektRequest.ManueltBeregnet::class, name = "MANUELT_BEREGNET"),
)
@JsonInclude(JsonInclude.Include.NON_NULL)
sealed class ArbeidstakerInntektRequest {
    abstract val begrunnelse: String?
    abstract val refusjon: List<Refusjonsperiode>?

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class Inntektsmelding(
        val inntektsmeldingId: String,
        override val begrunnelse: String? = null,
        override val refusjon: List<Refusjonsperiode>? = null,
    ) : ArbeidstakerInntektRequest()

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class Ainntekt(
        override val begrunnelse: String,
        override val refusjon: List<Refusjonsperiode>? = null,
    ) : ArbeidstakerInntektRequest()

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class Skjønnsfastsatt(
        val årsinntekt: InntektbeløpDto.Årlig,
        val årsak: ArbeidstakerSkjønnsfastsettelseÅrsak,
        override val begrunnelse: String,
        override val refusjon: List<Refusjonsperiode>? = null,
    ) : ArbeidstakerInntektRequest()

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class ManueltBeregnet(
        val årsinntekt: InntektbeløpDto.Årlig,
        override val begrunnelse: String,
        override val refusjon: List<Refusjonsperiode>? = null,
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
    abstract val begrunnelse: String

    data class PensjonsgivendeInntekt(
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
    abstract val begrunnelse: String

    data class Ainntekt(
        override val begrunnelse: String,
    ) : FrilanserInntektRequest()

    data class Skjønnsfastsatt(
        val årsinntekt: InntektbeløpDto.Årlig,
        val årsak: FrilanserSkjønnsfastsettelseÅrsak,
        override val begrunnelse: String,
    ) : FrilanserInntektRequest()
}

enum class FrilanserSkjønnsfastsettelseÅrsak {
    AVVIK_25_PROSENT,
    MANGELFULL_RAPPORTERING,
}

// ARBEIDSLEDIG
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = ArbeidsledigInntektRequest.Dagpenger::class, name = "DAGPENGER"),
    JsonSubTypes.Type(value = ArbeidsledigInntektRequest.Ventelønn::class, name = "VENTELONN"),
    JsonSubTypes.Type(value = ArbeidsledigInntektRequest.Vartpenger::class, name = "VARTPENGER"),
)
sealed class ArbeidsledigInntektRequest {
    abstract val begrunnelse: String

    data class Dagpenger(
        val dagbeløp: InntektbeløpDto.DagligInt,
        override val begrunnelse: String,
    ) : ArbeidsledigInntektRequest()

    data class Ventelønn(
        val årsinntekt: InntektbeløpDto.Årlig,
        override val begrunnelse: String,
    ) : ArbeidsledigInntektRequest()

    data class Vartpenger(
        val årsinntekt: InntektbeløpDto.Årlig,
        override val begrunnelse: String,
    ) : ArbeidsledigInntektRequest()
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
