package no.nav.helse.bakrommet.domain.sykepenger.sykepengegrunnlag

import no.nav.helse.bakrommet.domain.sykepenger.BeregningskoderKombinasjonerSykepengegrunnlag
import no.nav.helse.bakrommet.domain.sykepenger.BeregningskoderSykepengegrunnlag
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.util.*

sealed class SykepengegrunnlagBase(
    open val grunnbeløp: Inntekt,
    open val sykepengegrunnlag: Inntekt,
    open val seksG: Inntekt,
    open val begrensetTil6G: Boolean,
    open val grunnbeløpVirkningstidspunkt: LocalDate,
    open val beregningsgrunnlag: Inntekt,
)

data class Sykepengegrunnlag(
    override val grunnbeløp: Inntekt,
    override val sykepengegrunnlag: Inntekt,
    override val seksG: Inntekt,
    override val begrensetTil6G: Boolean,
    override val grunnbeløpVirkningstidspunkt: LocalDate,
    override val beregningsgrunnlag: Inntekt,
    val næringsdel: Næringsdel?,
    val kombinertBeregningskode: BeregningskoderKombinasjonerSykepengegrunnlag?,
) : SykepengegrunnlagBase(
        grunnbeløp = grunnbeløp,
        sykepengegrunnlag = sykepengegrunnlag,
        seksG = seksG,
        begrensetTil6G = begrensetTil6G,
        grunnbeløpVirkningstidspunkt = grunnbeløpVirkningstidspunkt,
        beregningsgrunnlag = beregningsgrunnlag,
    )

data class FrihåndSykepengegrunnlag(
    override val grunnbeløp: Inntekt,
    override val sykepengegrunnlag: Inntekt,
    override val seksG: Inntekt,
    override val begrensetTil6G: Boolean,
    override val grunnbeløpVirkningstidspunkt: LocalDate,
    override val beregningsgrunnlag: Inntekt,
    val begrunnelse: String,
    val beregningskoder: List<BeregningskoderSykepengegrunnlag>,
) : SykepengegrunnlagBase(
        grunnbeløp = grunnbeløp,
        sykepengegrunnlag = sykepengegrunnlag,
        seksG = seksG,
        begrensetTil6G = begrensetTil6G,
        grunnbeløpVirkningstidspunkt = grunnbeløpVirkningstidspunkt,
        beregningsgrunnlag = beregningsgrunnlag,
    )

data class Næringsdel(
    val pensjonsgivendeÅrsinntekt: Inntekt,
    val pensjonsgivendeÅrsinntekt6GBegrenset: Inntekt,
    val pensjonsgivendeÅrsinntektBegrensetTil6G: Boolean,
    val næringsdel: Inntekt,
    val sumAvArbeidsinntekt: Inntekt,
)

data class Sammenlikningsgrunnlag(
    val totaltSammenlikningsgrunnlag: Inntekt,
    val avvikProsent: Double,
    val avvikMotInntektsgrunnlag: Inntekt,
    val basertPåDokumentId: UUID,
)
