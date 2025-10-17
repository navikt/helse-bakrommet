package no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.helse.dto.InntektbeløpDto
import java.time.Year

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

    data class InaktivPensjonsgivende(
        override val omregnetÅrsinntekt: InntektbeløpDto.Årlig,
        override val sporing: String = "BEREGNINGSSPORINGVERDI",
        val pensjonsgivendeInntekt: PensjonsgivendeInntekt,
    ) : InntektData()

    data class InaktivSkjønnsfastsatt(
        override val omregnetÅrsinntekt: InntektbeløpDto.Årlig,
        override val sporing: String = "BEREGNINGSSPORINGVERDI",
    ) : InntektData()

    data class SelvstendigNæringsdrivendePensjonsgivende(
        override val omregnetÅrsinntekt: InntektbeløpDto.Årlig, // Denne oppdaterer seg ved at vi trekker arbeidstaker og frilans inntekt fra beregnetPensjonsgivendeInntekt ved beregning av sp grunnlaget
        val beregnetPensjonsgivendeInntekt: InntektbeløpDto.Årlig, // Dette er orginalen vi har beregnet fra sigrun data
        override val sporing: String = "BEREGNINGSSPORINGVERDI",
        val pensjonsgivendeInntekt: PensjonsgivendeInntekt,
    ) : InntektData()

    data class SelvstendigNæringsdrivendeSkjønnsfastsatt(
        override val omregnetÅrsinntekt: InntektbeløpDto.Årlig,
        override val sporing: String = "BEREGNINGSSPORINGVERDI",
    ) : InntektData()
}

data class PensjonsgivendeInntekt(
    val inntektAar: List<InntektAar>,
)

data class InntektAar(
    val aar: Year,
    val rapportertinntekt: InntektbeløpDto.Årlig,
    val inntektGrunnbelopsbegrenset: InntektbeløpDto.Årlig,
    val grunnbeløpAar: Year,
    val grunnbeløp: InntektbeløpDto,
)
