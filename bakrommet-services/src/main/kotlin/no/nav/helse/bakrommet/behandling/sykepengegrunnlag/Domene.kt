package no.nav.helse.bakrommet.behandling.sykepengegrunnlag

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.helse.bakrommet.BeregningskoderKombinasjonerSykepengegrunnlag
import no.nav.helse.bakrommet.BeregningskoderSykepengegrunnlag
import no.nav.helse.dto.InntektbeløpDto
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = Sykepengegrunnlag::class, name = "SYKEPENGEGRUNNLAG"),
    JsonSubTypes.Type(value = FrihåndSykepengegrunnlag::class, name = "FRIHÅND_SYKEPENGEGRUNNLAG"),
)
sealed class SykepengegrunnlagBase(
    open val grunnbeløp: InntektbeløpDto.Årlig,
    open val sykepengegrunnlag: InntektbeløpDto.Årlig,
    open val seksG: InntektbeløpDto.Årlig,
    open val begrensetTil6G: Boolean,
    open val grunnbeløpVirkningstidspunkt: LocalDate,
    open val beregningsgrunnlag: InntektbeløpDto.Årlig,
)

data class Sykepengegrunnlag(
    override val grunnbeløp: InntektbeløpDto.Årlig,
    override val sykepengegrunnlag: InntektbeløpDto.Årlig,
    override val seksG: InntektbeløpDto.Årlig,
    override val begrensetTil6G: Boolean,
    override val grunnbeløpVirkningstidspunkt: LocalDate,
    override val beregningsgrunnlag: InntektbeløpDto.Årlig,
    val næringsdel: Næringsdel?,
    val kombinertBeregningskode: BeregningskoderKombinasjonerSykepengegrunnlag? = null,
) : SykepengegrunnlagBase(
        grunnbeløp = grunnbeløp,
        sykepengegrunnlag = sykepengegrunnlag,
        seksG = seksG,
        begrensetTil6G = begrensetTil6G,
        grunnbeløpVirkningstidspunkt = grunnbeløpVirkningstidspunkt,
        beregningsgrunnlag = beregningsgrunnlag,
    )

data class FrihåndSykepengegrunnlag(
    override val grunnbeløp: InntektbeløpDto.Årlig,
    override val sykepengegrunnlag: InntektbeløpDto.Årlig,
    override val seksG: InntektbeløpDto.Årlig,
    override val begrensetTil6G: Boolean,
    override val grunnbeløpVirkningstidspunkt: LocalDate,
    override val beregningsgrunnlag: InntektbeløpDto.Årlig,
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
    val pensjonsgivendeÅrsinntekt: InntektbeløpDto.Årlig,
    val pensjonsgivendeÅrsinntekt6GBegrenset: InntektbeløpDto.Årlig,
    val pensjonsgivendeÅrsinntektBegrensetTil6G: Boolean,
    val næringsdel: InntektbeløpDto.Årlig,
    val sumAvArbeidsinntekt: InntektbeløpDto.Årlig,
)

data class Sammenlikningsgrunnlag(
    val totaltSammenlikningsgrunnlag: InntektbeløpDto.Årlig,
    val avvikProsent: Double,
    val avvikMotInntektsgrunnlag: InntektbeløpDto.Årlig,
    val basertPåDokumentId: UUID,
)

data class SykepengegrunnlagDbRecord(
    val sykepengegrunnlag: SykepengegrunnlagBase?,
    val sammenlikningsgrunnlag: Sammenlikningsgrunnlag?, // null for rene næringsdrivende mm++(?)
    val id: UUID,
    val opprettetAv: String,
    val opprettet: Instant,
    val oppdatert: Instant,
    val opprettetForBehandling: UUID,
    val låst: Boolean = false,
)

data class SykepengegrunnlagResponse(
    val sykepengegrunnlag: SykepengegrunnlagBase?,
    val sammenlikningsgrunnlag: Sammenlikningsgrunnlag?,
    val opprettetForBehandling: UUID,
    val låst: Boolean,
)
