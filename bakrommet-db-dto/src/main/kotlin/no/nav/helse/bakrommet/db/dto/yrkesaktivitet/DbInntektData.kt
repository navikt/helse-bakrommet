package no.nav.helse.bakrommet.db.dto.yrkesaktivitet

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.YearMonth

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "inntektstype")
@JsonSubTypes(
    JsonSubTypes.Type(value = DbInntektData.ArbeidstakerInntektsmelding::class, name = "ARBEIDSTAKER_INNTEKTSMELDING"),
    JsonSubTypes.Type(value = DbInntektData.ArbeidstakerAinntekt::class, name = "ARBEIDSTAKER_AINNTEKT"),
    JsonSubTypes.Type(value = DbInntektData.ArbeidstakerSkjønnsfastsatt::class, name = "ARBEIDSTAKER_SKJØNNSFASTSATT"),
    JsonSubTypes.Type(value = DbInntektData.FrilanserAinntekt::class, name = "FRILANSER_AINNTEKT"),
    JsonSubTypes.Type(value = DbInntektData.FrilanserSkjønnsfastsatt::class, name = "FRILANSER_SKJØNNSFASTSATT"),
    JsonSubTypes.Type(value = DbInntektData.Arbeidsledig::class, name = "ARBEIDSLEDIG"),
    JsonSubTypes.Type(value = DbInntektData.InaktivPensjonsgivende::class, name = "INAKTIV_PENSJONSGIVENDE"),
    JsonSubTypes.Type(value = DbInntektData.InaktivSkjønnsfastsatt::class, name = "INAKTIV_SKJØNNSFASTSATT"),
    JsonSubTypes.Type(value = DbInntektData.SelvstendigNæringsdrivendePensjonsgivende::class, name = "SELVSTENDIG_NÆRINGSDRIVENDE_PENSJONSGIVENDE"),
    JsonSubTypes.Type(value = DbInntektData.SelvstendigNæringsdrivendeSkjønnsfastsatt::class, name = "SELVSTENDIG_NÆRINGSDRIVENDE_SKJØNNSFASTSATT"),
)
sealed class DbInntektData {
    abstract val omregnetÅrsinntekt: Double
    abstract val sporing: DbBeregningskoderSykepengegrunnlag

    data class ArbeidstakerInntektsmelding(
        val inntektsmeldingId: String,
        val inntektsmelding: String,
        override val omregnetÅrsinntekt: Double,
        override val sporing: DbBeregningskoderSykepengegrunnlag,
    ) : DbInntektData()

    data class ArbeidstakerAinntekt(
        override val omregnetÅrsinntekt: Double,
        override val sporing: DbBeregningskoderSykepengegrunnlag,
        val kildedata: Map<YearMonth, Double>,
        // TODO legg med litt kilder
    ) : DbInntektData()

    data class ArbeidstakerSkjønnsfastsatt(
        override val omregnetÅrsinntekt: Double,
        override val sporing: DbBeregningskoderSykepengegrunnlag,
    ) : DbInntektData()

    data class FrilanserAinntekt(
        override val omregnetÅrsinntekt: Double,
        override val sporing: DbBeregningskoderSykepengegrunnlag,
        val kildedata: Map<YearMonth, Double>,
    ) : DbInntektData()

    data class FrilanserSkjønnsfastsatt(
        override val omregnetÅrsinntekt: Double,
        override val sporing: DbBeregningskoderSykepengegrunnlag,
    ) : DbInntektData()

    data class Arbeidsledig(
        override val omregnetÅrsinntekt: Double,
        override val sporing: DbBeregningskoderSykepengegrunnlag,
    ) : DbInntektData()

    data class InaktivSkjønnsfastsatt(
        override val omregnetÅrsinntekt: Double,
        override val sporing: DbBeregningskoderSykepengegrunnlag,
    ) : DbInntektData()

    data class PensjonsgivendeInntekt(
        val omregnetÅrsinntekt: Double,
        val pensjonsgivendeInntekt: List<DbInntektÅr>,
        val anvendtGrunnbeløp: Double,
    )

    data class InaktivPensjonsgivende(
        override val omregnetÅrsinntekt: Double,
        override val sporing: DbBeregningskoderSykepengegrunnlag,
        val pensjonsgivendeInntekt: PensjonsgivendeInntekt,
    ) : DbInntektData()

    data class SelvstendigNæringsdrivendePensjonsgivende(
        override val omregnetÅrsinntekt: Double,
        override val sporing: DbBeregningskoderSykepengegrunnlag,
        val pensjonsgivendeInntekt: PensjonsgivendeInntekt,
    ) : DbInntektData()

    data class SelvstendigNæringsdrivendeSkjønnsfastsatt(
        override val omregnetÅrsinntekt: Double,
        override val sporing: DbBeregningskoderSykepengegrunnlag,
    ) : DbInntektData()
}
