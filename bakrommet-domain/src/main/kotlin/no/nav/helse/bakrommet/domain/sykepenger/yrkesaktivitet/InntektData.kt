package no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet

import no.nav.helse.bakrommet.domain.sykepenger.BeregningskoderSykepengegrunnlag
import no.nav.helse.økonomi.Inntekt
import java.time.Year
import java.time.YearMonth

sealed class InntektData {
    abstract val omregnetÅrsinntekt: Inntekt
    abstract val sporing: BeregningskoderSykepengegrunnlag

    data class ArbeidstakerInntektsmelding(
        val inntektsmeldingId: String,
        val inntektsmelding: String,
        override val omregnetÅrsinntekt: Inntekt,
        override val sporing: BeregningskoderSykepengegrunnlag = BeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_HOVEDREGEL,
    ) : InntektData()

    data class ArbeidstakerAinntekt(
        override val omregnetÅrsinntekt: Inntekt,
        override val sporing: BeregningskoderSykepengegrunnlag = BeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_HOVEDREGEL,
        val kildedata: Map<YearMonth, Inntekt>,
        // TODO legg med litt kilder
    ) : InntektData()

    data class ArbeidstakerSkjønnsfastsatt(
        override val omregnetÅrsinntekt: Inntekt,
        override val sporing: BeregningskoderSykepengegrunnlag,
    ) : InntektData()

    data class FrilanserAinntekt(
        override val omregnetÅrsinntekt: Inntekt,
        override val sporing: BeregningskoderSykepengegrunnlag = BeregningskoderSykepengegrunnlag.FRILANSER_SYKEPENGEGRUNNLAG_HOVEDREGEL,
        val kildedata: Map<YearMonth, Inntekt>,
    ) : InntektData()

    data class FrilanserSkjønnsfastsatt(
        override val omregnetÅrsinntekt: Inntekt,
        override val sporing: BeregningskoderSykepengegrunnlag,
    ) : InntektData()

    data class Arbeidsledig(
        override val omregnetÅrsinntekt: Inntekt,
        override val sporing: BeregningskoderSykepengegrunnlag,
    ) : InntektData()

    data class InaktivSkjønnsfastsatt(
        override val omregnetÅrsinntekt: Inntekt,
        override val sporing: BeregningskoderSykepengegrunnlag,
    ) : InntektData()

    data class PensjonsgivendeInntekt(
        val omregnetÅrsinntekt: Inntekt,
        val pensjonsgivendeInntekt: List<InntektAar>,
        val anvendtGrunnbeløp: Inntekt,
    )

    data class InaktivPensjonsgivende(
        override val omregnetÅrsinntekt: Inntekt,
        override val sporing: BeregningskoderSykepengegrunnlag = BeregningskoderSykepengegrunnlag.INAKTIV_SYKEPENGEGRUNNLAG_HOVEDREGEL,
        val pensjonsgivendeInntekt: PensjonsgivendeInntekt,
    ) : InntektData()

    data class SelvstendigNæringsdrivendePensjonsgivende(
        override val omregnetÅrsinntekt: Inntekt,
        override val sporing: BeregningskoderSykepengegrunnlag = BeregningskoderSykepengegrunnlag.SELVSTENDIG_SYKEPENGEGRUNNLAG_HOVEDREGEL,
        val pensjonsgivendeInntekt: PensjonsgivendeInntekt,
    ) : InntektData()

    data class SelvstendigNæringsdrivendeSkjønnsfastsatt(
        override val omregnetÅrsinntekt: Inntekt,
        override val sporing: BeregningskoderSykepengegrunnlag,
    ) : InntektData()
}

data class InntektAar(
    val år: Year,
    val rapportertinntekt: Inntekt,
    val justertÅrsgrunnlag: Inntekt,
    val antallGKompensert: Double,
    val snittG: Inntekt,
)
