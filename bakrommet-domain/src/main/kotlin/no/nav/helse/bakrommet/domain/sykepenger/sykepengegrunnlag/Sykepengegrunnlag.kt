package no.nav.helse.bakrommet.domain.sykepenger.sykepengegrunnlag

import no.nav.helse.bakrommet.domain.sykepenger.BeregningskoderKombinasjonerSykepengegrunnlag
import no.nav.helse.bakrommet.domain.sykepenger.BeregningskoderSykepengegrunnlag
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.util.UUID

@JvmInline
value class SykepengegrunnlagId(
    val value: UUID,
)

sealed interface SykepengegrunnlagBase {
    val id: SykepengegrunnlagId
    val grunnbeløp: Inntekt
    val sykepengegrunnlag: Inntekt
    val seksG: Inntekt
    val begrensetTil6G: Boolean
    val grunnbeløpVirkningstidspunkt: LocalDate
    val beregningsgrunnlag: Inntekt
}

data class Sykepengegrunnlag(
    override val id: SykepengegrunnlagId,
    override val grunnbeløp: Inntekt,
    override val sykepengegrunnlag: Inntekt,
    override val seksG: Inntekt,
    override val begrensetTil6G: Boolean,
    override val grunnbeløpVirkningstidspunkt: LocalDate,
    override val beregningsgrunnlag: Inntekt,
    val næringsdel: Næringsdel?,
    val kombinertBeregningskode: BeregningskoderKombinasjonerSykepengegrunnlag?,
) : SykepengegrunnlagBase {
    data class Næringsdel(
        val pensjonsgivendeÅrsinntekt: Inntekt,
        val pensjonsgivendeÅrsinntekt6GBegrenset: Inntekt,
        val pensjonsgivendeÅrsinntektBegrensetTil6G: Boolean,
        val næringsdel: Inntekt,
        val sumAvArbeidsinntekt: Inntekt,
    )
}

data class FrihåndSykepengegrunnlag(
    override val id: SykepengegrunnlagId,
    override val grunnbeløp: Inntekt,
    override val sykepengegrunnlag: Inntekt,
    override val seksG: Inntekt,
    override val begrensetTil6G: Boolean,
    override val grunnbeløpVirkningstidspunkt: LocalDate,
    override val beregningsgrunnlag: Inntekt,
    val begrunnelse: String,
    val beregningskoder: List<BeregningskoderSykepengegrunnlag>,
) : SykepengegrunnlagBase
