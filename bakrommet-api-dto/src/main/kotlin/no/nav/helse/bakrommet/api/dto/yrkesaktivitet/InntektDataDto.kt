package no.nav.helse.bakrommet.api.dto.yrkesaktivitet

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.JsonNode

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "inntektstype")
@JsonSubTypes(
    JsonSubTypes.Type(value = InntektDataDto.ArbeidstakerInntektsmelding::class, name = "ARBEIDSTAKER_INNTEKTSMELDING"),
    JsonSubTypes.Type(value = InntektDataDto.ArbeidstakerAinntekt::class, name = "ARBEIDSTAKER_AINNTEKT"),
    JsonSubTypes.Type(value = InntektDataDto.ArbeidstakerSkjønnsfastsatt::class, name = "ARBEIDSTAKER_SKJØNNSFASTSATT"),
    JsonSubTypes.Type(value = InntektDataDto.FrilanserAinntekt::class, name = "FRILANSER_AINNTEKT"),
    JsonSubTypes.Type(value = InntektDataDto.FrilanserSkjønnsfastsatt::class, name = "FRILANSER_SKJØNNSFASTSATT"),
    JsonSubTypes.Type(value = InntektDataDto.Arbeidsledig::class, name = "ARBEIDSLEDIG"),
    JsonSubTypes.Type(value = InntektDataDto.InaktivPensjonsgivende::class, name = "INAKTIV_PENSJONSGIVENDE"),
    JsonSubTypes.Type(value = InntektDataDto.InaktivSkjønnsfastsatt::class, name = "INAKTIV_SKJØNNSFASTSATT"),
    JsonSubTypes.Type(value = InntektDataDto.SelvstendigNæringsdrivendePensjonsgivende::class, name = "SELVSTENDIG_NÆRINGSDRIVENDE_PENSJONSGIVENDE"),
    JsonSubTypes.Type(value = InntektDataDto.SelvstendigNæringsdrivendeSkjønnsfastsatt::class, name = "SELVSTENDIG_NÆRINGSDRIVENDE_SKJØNNSFASTSATT"),
)
sealed class InntektDataDto {
    abstract val omregnetÅrsinntekt: Double // Årlig beløp som Double i stedet for InntektbeløpDto.Årlig
    abstract val sporing: String

    data class ArbeidstakerInntektsmelding(
        val inntektsmeldingId: String,
        override val omregnetÅrsinntekt: Double,
        val inntektsmelding: JsonNode,
        override val sporing: String,
    ) : InntektDataDto()

    data class ArbeidstakerAinntekt(
        override val omregnetÅrsinntekt: Double,
        override val sporing: String,
        val kildedata: Map<String, Double>, // Map<YearMonth, MånedligDouble> som Map<String, Double>
    ) : InntektDataDto()

    data class ArbeidstakerSkjønnsfastsatt(
        override val omregnetÅrsinntekt: Double,
        override val sporing: String,
    ) : InntektDataDto()

    data class FrilanserAinntekt(
        override val omregnetÅrsinntekt: Double,
        override val sporing: String,
        val kildedata: Map<String, Double>, // Map<YearMonth, MånedligDouble> som Map<String, Double>
    ) : InntektDataDto()

    data class FrilanserSkjønnsfastsatt(
        override val omregnetÅrsinntekt: Double,
        override val sporing: String,
    ) : InntektDataDto()

    data class Arbeidsledig(
        override val omregnetÅrsinntekt: Double,
        override val sporing: String,
    ) : InntektDataDto()

    data class InaktivSkjønnsfastsatt(
        override val omregnetÅrsinntekt: Double,
        override val sporing: String,
    ) : InntektDataDto()

    data class InaktivPensjonsgivende(
        override val omregnetÅrsinntekt: Double,
        override val sporing: String,
        val pensjonsgivendeInntekt: PensjonsgivendeInntektDto,
    ) : InntektDataDto()

    data class SelvstendigNæringsdrivendePensjonsgivende(
        override val omregnetÅrsinntekt: Double,
        override val sporing: String,
        val pensjonsgivendeInntekt: PensjonsgivendeInntektDto,
    ) : InntektDataDto()

    data class SelvstendigNæringsdrivendeSkjønnsfastsatt(
        override val omregnetÅrsinntekt: Double,
        override val sporing: String,
    ) : InntektDataDto()
}

data class PensjonsgivendeInntektDto(
    val omregnetÅrsinntekt: Double,
    val pensjonsgivendeInntekt: List<InntektAarDto>,
    val anvendtGrunnbeløp: Double,
)

data class InntektAarDto(
    val år: String, // Year som String
    val rapportertinntekt: Double, // Årlig beløp som Double
    val justertÅrsgrunnlag: Double, // Årlig beløp som Double
    val antallGKompensert: Double,
    val snittG: Double, // Årlig beløp som Double
)
