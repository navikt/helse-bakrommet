package no.nav.helse.bakrommet.db.dto.sykepengegrunnlag

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.helse.bakrommet.db.dto.yrkesaktivitet.DbInntekt
import java.time.LocalDate
import java.util.UUID

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = DbSykepengegrunnlag::class, name = "SYKEPENGEGRUNNLAG"),
    JsonSubTypes.Type(value = DbFrihåndSykepengegrunnlag::class, name = "FRIHÅND_SYKEPENGEGRUNNLAG"),
)
sealed class DbSykepengegrunnlagBase(
    open val grunnbeløp: DbInntekt.Årlig,
    open val sykepengegrunnlag: DbInntekt.Årlig,
    open val seksG: DbInntekt.Årlig,
    open val begrensetTil6G: Boolean,
    open val grunnbeløpVirkningstidspunkt: LocalDate,
    open val beregningsgrunnlag: DbInntekt.Årlig,
)

data class DbSykepengegrunnlag(
    override val grunnbeløp: DbInntekt.Årlig,
    override val sykepengegrunnlag: DbInntekt.Årlig,
    override val seksG: DbInntekt.Årlig,
    override val begrensetTil6G: Boolean,
    override val grunnbeløpVirkningstidspunkt: LocalDate,
    override val beregningsgrunnlag: DbInntekt.Årlig,
    val næringsdel: DbNæringsdel?,
    val kombinertBeregningskode: DbBeregningskoderKombinasjonerSykepengegrunnlag? = null,
) : DbSykepengegrunnlagBase(
        grunnbeløp = grunnbeløp,
        sykepengegrunnlag = sykepengegrunnlag,
        seksG = seksG,
        begrensetTil6G = begrensetTil6G,
        grunnbeløpVirkningstidspunkt = grunnbeløpVirkningstidspunkt,
        beregningsgrunnlag = beregningsgrunnlag,
    )

data class DbFrihåndSykepengegrunnlag(
    override val grunnbeløp: DbInntekt.Årlig,
    override val sykepengegrunnlag: DbInntekt.Årlig,
    override val seksG: DbInntekt.Årlig,
    override val begrensetTil6G: Boolean,
    override val grunnbeløpVirkningstidspunkt: LocalDate,
    override val beregningsgrunnlag: DbInntekt.Årlig,
    val begrunnelse: String,
    val beregningskoder: List<DbBeregningskoderSykepengegrunnlag>,
) : DbSykepengegrunnlagBase(
        grunnbeløp = grunnbeløp,
        sykepengegrunnlag = sykepengegrunnlag,
        seksG = seksG,
        begrensetTil6G = begrensetTil6G,
        grunnbeløpVirkningstidspunkt = grunnbeløpVirkningstidspunkt,
        beregningsgrunnlag = beregningsgrunnlag,
    )

data class DbNæringsdel(
    val pensjonsgivendeÅrsinntekt: DbInntekt.Årlig,
    val pensjonsgivendeÅrsinntekt6GBegrenset: DbInntekt.Årlig,
    val pensjonsgivendeÅrsinntektBegrensetTil6G: Boolean,
    val næringsdel: DbInntekt.Årlig,
    val sumAvArbeidsinntekt: DbInntekt.Årlig,
)

data class DbSammenlikningsgrunnlag(
    val totaltSammenlikningsgrunnlag: DbInntekt.Årlig,
    val avvikProsent: Double,
    val avvikMotInntektsgrunnlag: DbInntekt.Årlig,
    val basertPåDokumentId: UUID,
)

enum class DbBeregningskoderSykepengegrunnlag {
    ARBEIDSTAKER_SYKEPENGEGRUNNLAG_HOVEDREGEL,
    ARBEIDSTAKER_SYKEPENGEGRUNNLAG_SKJOENN_AVVIK,
    ARBEIDSTAKER_SYKEPENGEGRUNNLAG_SKJOENN_URIKTIG,
    FRILANSER_SYKEPENGEGRUNNLAG_HOVEDREGEL,
    FRILANSER_SYKEPENGEGRUNNLAG_SKJOENN_AVVIK,
    FRILANSER_SYKEPENGEGRUNNLAG_SKJOENN_URIKTIG,
    SELVSTENDIG_SYKEPENGEGRUNNLAG_HOVEDREGEL,
    SELVSTENDIG_SYKEPENGEGRUNNLAG_SKJOENN_VARIG_ENDRING,
    SELVSTENDIG_SYKEPENGEGRUNNLAG_SKJOENN_NYIARB,
    INAKTIV_SYKEPENGEGRUNNLAG_HOVEDREGEL,
    INAKTIV_SYKEPENGEGRUNNLAG_SKJOENN_VARIG_ENDRING,
    INAKTIV_SYKEPENGEGRUNNLAG_SKJOENN_NYIARB,
    ARBEIDSLEDIG_SYKEPENGEGRUNNLAG_DAGPENGER,
    ARBEIDSLEDIG_SYKEPENGEGRUNNLAG_VENTELOENN,
    ARBEIDSLEDIG_SYKEPENGEGRUNNLAG_VARTPENGER,
    ARBEIDSTAKER_SYKEPENGEGRUNNLAG_TIDSBEGRENSET_FOER_SLUTTDATO,
}

enum class DbBeregningskoderKombinasjonerSykepengegrunnlag {
    KOMBINERT_ARBEIDSTAKER_SELVSTENDIG_SYKEPENGEGRUNNLAG,
    KOMBINERT_ARBEIDSTAKER_FRILANSER_SYKEPENGEGRUNNLAG,
    KOMBINERT_ARBEIDSTAKER_FRILANSER_SELVSTENDIG_SYKEPENGEGRUNNLAG,
    KOMBINERT_SELVSTENDIG_FRILANSER_SYKEPENGEGRUNNLAG,
}
