package no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter

import no.nav.helse.dto.InntektbeløpDto
import java.time.Year

sealed class InntektData {
    abstract val omregnetÅrsinntekt: InntektbeløpDto.Årlig
    abstract val sporing: String

    data class ArbeidstakerInntektsmelding(
        val inntektsmeldingId: String,
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

    data class SelvstendigNæringsdrivendePensjonsgivende(
        override val omregnetÅrsinntekt: InntektbeløpDto.Årlig,
        override val sporing: String = "BEREGNINGSSPORINGVERDI",
        val pensjonsgivendeInntekt: PensjonsgivendeInntekt,
    ) : InntektData()

    data class SelvstendigNæringsdrivendeSkjønnsfastsatt(
        override val omregnetÅrsinntekt: InntektbeløpDto.Årlig,
        override val sporing: String = "BEREGNINGSSPORINGVERDI",
        val pensjonsgivendeInntekt: PensjonsgivendeInntekt,
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
