package no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag

import no.nav.helse.bakrommet.BeregningskoderKombinasjonerSykepengegrunnlag
import no.nav.helse.dto.InntektbeløpDto
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class Sykepengegrunnlag(
    val grunnbeløp: InntektbeløpDto.Årlig,
    val totaltInntektsgrunnlag: InntektbeløpDto.Årlig,
    val sykepengegrunnlag: InntektbeløpDto.Årlig,
    val seksG: InntektbeløpDto.Årlig,
    val begrensetTil6G: Boolean,
    val grunnbeløpVirkningstidspunkt: LocalDate,
    val næringsdel: Næringsdel?,
    val kombinertBeregningskode: BeregningskoderKombinasjonerSykepengegrunnlag? = null,
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
    val sykepengegrunnlag: Sykepengegrunnlag?,
    val sammenlikningsgrunnlag: Sammenlikningsgrunnlag?, // null for rene næringsdrivende mm++(?)
    val id: UUID,
    val opprettetAv: String,
    val opprettet: Instant,
    val oppdatert: Instant,
)

data class SykepengegrunnlagResponse(
    val sykepengegrunnlag: Sykepengegrunnlag?,
    val sammenlikningsgrunnlag: Sammenlikningsgrunnlag?,
)
