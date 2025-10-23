package no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.dto.InntektbeløpDto
import java.time.Year
import java.time.YearMonth

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "inntektstype")
@JsonSubTypes(
    JsonSubTypes.Type(value = InntektData.ArbeidstakerInntektsmelding::class, name = "ARBEIDSTAKER_INNTEKTSMELDING"),
    JsonSubTypes.Type(value = InntektData.ArbeidstakerAinntekt::class, name = "ARBEIDSTAKER_AINNTEKT"),
    JsonSubTypes.Type(value = InntektData.ArbeidstakerSkjønnsfastsatt::class, name = "ARBEIDSTAKER_SKJØNNSFASTSATT"),
    JsonSubTypes.Type(value = InntektData.FrilanserAinntekt::class, name = "FRILANSER_AINNTEKT"),
    JsonSubTypes.Type(value = InntektData.FrilanserSkjønnsfastsatt::class, name = "FRILANSER_SKJØNNSFASTSATT"),
    JsonSubTypes.Type(value = InntektData.Arbeidsledig::class, name = "ARBEIDSLEDIG"),
    JsonSubTypes.Type(value = InntektData.InaktivPensjonsgivende::class, name = "INAKTIV_PENSJONSGIVENDE"),
    JsonSubTypes.Type(value = InntektData.InaktivSkjønnsfastsatt::class, name = "INAKTIV_SKJØNNSFASTSATT"),
    JsonSubTypes.Type(value = InntektData.ArbeidstakerManueltBeregnet::class, name = "ARBEIDSTAKER_MANUELT_BEREGNET"),
    JsonSubTypes.Type(value = InntektData.SelvstendigNæringsdrivendePensjonsgivende::class, name = "SELVSTENDIG_NÆRINGSDRIVENDE_PENSJONSGIVENDE"),
    JsonSubTypes.Type(value = InntektData.SelvstendigNæringsdrivendeSkjønnsfastsatt::class, name = "SELVSTENDIG_NÆRINGSDRIVENDE_SKJØNNSFASTSATT"),
)
sealed class InntektData {
    abstract val omregnetÅrsinntekt: InntektbeløpDto.Årlig
    abstract val sporing: String

    data class ArbeidstakerInntektsmelding(
        val inntektsmeldingId: String,
        val inntektsmelding: JsonNode,
        override val omregnetÅrsinntekt: InntektbeløpDto.Årlig,
        override val sporing: String = "BEREGNINGSSPORINGVERDI",
    ) : InntektData()

    data class ArbeidstakerManueltBeregnet(
        override val omregnetÅrsinntekt: InntektbeløpDto.Årlig,
        override val sporing: String = "BEREGNINGSSPORINGVERDI",
    ) : InntektData()

    data class ArbeidstakerAinntekt(
        override val omregnetÅrsinntekt: InntektbeløpDto.Årlig,
        override val sporing: String = "BEREGNINGSSPORINGVERDI",
        val kildedata: Map<YearMonth, InntektbeløpDto.MånedligDouble>,
        // TODO legg med litt kilder
    ) : InntektData()

    data class ArbeidstakerSkjønnsfastsatt(
        override val omregnetÅrsinntekt: InntektbeløpDto.Årlig,
        override val sporing: String = "BEREGNINGSSPORINGVERDI",
    ) : InntektData()

    data class FrilanserAinntekt(
        override val omregnetÅrsinntekt: InntektbeløpDto.Årlig,
        override val sporing: String = "BEREGNINGSSPORINGVERDI",
        // TODO legg med litt kilder
    ) : InntektData()

    data class FrilanserSkjønnsfastsatt(
        override val omregnetÅrsinntekt: InntektbeløpDto.Årlig,
        override val sporing: String = "BEREGNINGSSPORINGVERDI",
    ) : InntektData()

    data class Arbeidsledig(
        override val omregnetÅrsinntekt: InntektbeløpDto.Årlig,
        override val sporing: String = "BEREGNINGSSPORINGVERDI",
    ) : InntektData()

    data class InaktivSkjønnsfastsatt(
        override val omregnetÅrsinntekt: InntektbeløpDto.Årlig,
        override val sporing: String = "BEREGNINGSSPORINGVERDI",
    ) : InntektData()

    data class PensjonsgivendeInntekt(
        val omregnetÅrsinntekt: InntektbeløpDto.Årlig,
        val pensjonsgivendeInntekt: List<InntektAar>,
        val anvendtGrunnbeløp: InntektbeløpDto.Årlig,
    )

    data class InaktivPensjonsgivende(
        override val omregnetÅrsinntekt: InntektbeløpDto.Årlig,
        override val sporing: String = "BEREGNINGSSPORINGVERDI",
        val pensjonsgivendeInntekt: PensjonsgivendeInntekt,
    ) : InntektData()

    data class SelvstendigNæringsdrivendePensjonsgivende(
        override val omregnetÅrsinntekt: InntektbeløpDto.Årlig, // Denne oppdaterer seg ved at vi trekker arbeidstaker og frilans inntekt fra beregnetPensjonsgivendeInntekt ved beregning av sp grunnlaget
        override val sporing: String = "BEREGNINGSSPORINGVERDI",
        val pensjonsgivendeInntekt: PensjonsgivendeInntekt,
    ) : InntektData()

    data class SelvstendigNæringsdrivendeSkjønnsfastsatt(
        override val omregnetÅrsinntekt: InntektbeløpDto.Årlig,
        override val sporing: String = "BEREGNINGSSPORINGVERDI",
    ) : InntektData()
}

data class InntektAar(
    val år: Year,
    val rapportertinntekt: InntektbeløpDto.Årlig,
    val justertÅrsgrunnlag: InntektbeløpDto.Årlig,
    val antallGKompensert: Double,
    val snittG: InntektbeløpDto.Årlig,
)
