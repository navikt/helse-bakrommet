package no.nav.helse.bakrommet.behandling.inntekter

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.bakrommet.BeregningskoderSykepengegrunnlag
import no.nav.helse.bakrommet.BeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_HOVEDREGEL
import no.nav.helse.bakrommet.BeregningskoderSykepengegrunnlag.FRILANSER_SYKEPENGEGRUNNLAG_HOVEDREGEL
import no.nav.helse.dto.InntektbeløpDto
import java.time.Year
import java.time.YearMonth

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "inntektstype")
@JsonSubTypes(
    JsonSubTypes.Type(value = InntektDataOld.ArbeidstakerInntektsmelding::class, name = "ARBEIDSTAKER_INNTEKTSMELDING"),
    JsonSubTypes.Type(value = InntektDataOld.ArbeidstakerAinntekt::class, name = "ARBEIDSTAKER_AINNTEKT"),
    JsonSubTypes.Type(value = InntektDataOld.ArbeidstakerSkjønnsfastsatt::class, name = "ARBEIDSTAKER_SKJØNNSFASTSATT"),
    JsonSubTypes.Type(value = InntektDataOld.FrilanserAinntekt::class, name = "FRILANSER_AINNTEKT"),
    JsonSubTypes.Type(value = InntektDataOld.FrilanserSkjønnsfastsatt::class, name = "FRILANSER_SKJØNNSFASTSATT"),
    JsonSubTypes.Type(value = InntektDataOld.Arbeidsledig::class, name = "ARBEIDSLEDIG"),
    JsonSubTypes.Type(value = InntektDataOld.InaktivPensjonsgivende::class, name = "INAKTIV_PENSJONSGIVENDE"),
    JsonSubTypes.Type(value = InntektDataOld.InaktivSkjønnsfastsatt::class, name = "INAKTIV_SKJØNNSFASTSATT"),
    JsonSubTypes.Type(value = InntektDataOld.SelvstendigNæringsdrivendePensjonsgivende::class, name = "SELVSTENDIG_NÆRINGSDRIVENDE_PENSJONSGIVENDE"),
    JsonSubTypes.Type(value = InntektDataOld.SelvstendigNæringsdrivendeSkjønnsfastsatt::class, name = "SELVSTENDIG_NÆRINGSDRIVENDE_SKJØNNSFASTSATT"),
)
sealed class InntektDataOld {
    abstract val omregnetÅrsinntekt: InntektbeløpDto.Årlig
    abstract val sporing: BeregningskoderSykepengegrunnlag

    data class ArbeidstakerInntektsmelding(
        val inntektsmeldingId: String,
        val inntektsmelding: JsonNode,
        override val omregnetÅrsinntekt: InntektbeløpDto.Årlig,
        override val sporing: BeregningskoderSykepengegrunnlag = ARBEIDSTAKER_SYKEPENGEGRUNNLAG_HOVEDREGEL,
    ) : InntektDataOld()

    data class ArbeidstakerAinntekt(
        override val omregnetÅrsinntekt: InntektbeløpDto.Årlig,
        override val sporing: BeregningskoderSykepengegrunnlag = ARBEIDSTAKER_SYKEPENGEGRUNNLAG_HOVEDREGEL,
        val kildedata: Map<YearMonth, InntektbeløpDto.MånedligDouble>,
        // TODO legg med litt kilder
    ) : InntektDataOld()

    data class ArbeidstakerSkjønnsfastsatt(
        override val omregnetÅrsinntekt: InntektbeløpDto.Årlig,
        override val sporing: BeregningskoderSykepengegrunnlag,
    ) : InntektDataOld()

    data class FrilanserAinntekt(
        override val omregnetÅrsinntekt: InntektbeløpDto.Årlig,
        override val sporing: BeregningskoderSykepengegrunnlag = FRILANSER_SYKEPENGEGRUNNLAG_HOVEDREGEL,
        val kildedata: Map<YearMonth, InntektbeløpDto.MånedligDouble>,
    ) : InntektDataOld()

    data class FrilanserSkjønnsfastsatt(
        override val omregnetÅrsinntekt: InntektbeløpDto.Årlig,
        override val sporing: BeregningskoderSykepengegrunnlag,
    ) : InntektDataOld()

    data class Arbeidsledig(
        override val omregnetÅrsinntekt: InntektbeløpDto.Årlig,
        override val sporing: BeregningskoderSykepengegrunnlag,
    ) : InntektDataOld()

    data class InaktivSkjønnsfastsatt(
        override val omregnetÅrsinntekt: InntektbeløpDto.Årlig,
        override val sporing: BeregningskoderSykepengegrunnlag,
    ) : InntektDataOld()

    data class PensjonsgivendeInntekt(
        val omregnetÅrsinntekt: InntektbeløpDto.Årlig,
        val pensjonsgivendeInntekt: List<InntektAarOld>,
        val anvendtGrunnbeløp: InntektbeløpDto.Årlig,
    )

    data class InaktivPensjonsgivende(
        override val omregnetÅrsinntekt: InntektbeløpDto.Årlig,
        override val sporing: BeregningskoderSykepengegrunnlag = BeregningskoderSykepengegrunnlag.INAKTIV_SYKEPENGEGRUNNLAG_HOVEDREGEL,
        val pensjonsgivendeInntekt: PensjonsgivendeInntekt,
    ) : InntektDataOld()

    data class SelvstendigNæringsdrivendePensjonsgivende(
        override val omregnetÅrsinntekt: InntektbeløpDto.Årlig,
        override val sporing: BeregningskoderSykepengegrunnlag = BeregningskoderSykepengegrunnlag.SELVSTENDIG_SYKEPENGEGRUNNLAG_HOVEDREGEL,
        val pensjonsgivendeInntekt: PensjonsgivendeInntekt,
    ) : InntektDataOld()

    data class SelvstendigNæringsdrivendeSkjønnsfastsatt(
        override val omregnetÅrsinntekt: InntektbeløpDto.Årlig,
        override val sporing: BeregningskoderSykepengegrunnlag,
    ) : InntektDataOld()
}

data class InntektAarOld(
    val år: Year,
    val rapportertinntekt: InntektbeløpDto.Årlig,
    val justertÅrsgrunnlag: InntektbeløpDto.Årlig,
    val antallGKompensert: Double,
    val snittG: InntektbeløpDto.Årlig,
)
