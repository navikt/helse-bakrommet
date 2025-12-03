package no.nav.helse.bakrommet.api.sykepengegrunnlag

import no.nav.helse.bakrommet.BeregningskoderKombinasjonerSykepengegrunnlag
import no.nav.helse.bakrommet.BeregningskoderSykepengegrunnlag
import no.nav.helse.bakrommet.api.dto.sykepengegrunnlag.BeregningskoderKombinasjonerSykepengegrunnlagDto
import no.nav.helse.bakrommet.api.dto.sykepengegrunnlag.BeregningskoderSykepengegrunnlagDto
import no.nav.helse.bakrommet.api.dto.sykepengegrunnlag.FrihåndSykepengegrunnlagDto
import no.nav.helse.bakrommet.api.dto.sykepengegrunnlag.NæringsdelDto
import no.nav.helse.bakrommet.api.dto.sykepengegrunnlag.OpprettSykepengegrunnlagRequestDto
import no.nav.helse.bakrommet.api.dto.sykepengegrunnlag.SammenlikningsgrunnlagDto
import no.nav.helse.bakrommet.api.dto.sykepengegrunnlag.SykepengegrunnlagDto
import no.nav.helse.bakrommet.api.dto.sykepengegrunnlag.SykepengegrunnlagResponseDto
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.FrihåndSykepengegrunnlag
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.Næringsdel
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.OpprettSykepengegrunnlagRequest
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.Sammenlikningsgrunnlag
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.Sykepengegrunnlag
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.SykepengegrunnlagBase
import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.SykepengegrunnlagResponse
import java.math.BigDecimal

fun OpprettSykepengegrunnlagRequestDto.tilOpprettSykepengegrunnlagRequest(): OpprettSykepengegrunnlagRequest =
    OpprettSykepengegrunnlagRequest(
        beregningsgrunnlag = BigDecimal.valueOf(beregningsgrunnlag),
        begrunnelse = begrunnelse,
        datoForGBegrensning = datoForGBegrensning,
        beregningskoder = beregningskoder.map { it.tilBeregningskoderSykepengegrunnlag() },
    )

fun SykepengegrunnlagResponse.tilSykepengegrunnlagResponseDto(): SykepengegrunnlagResponseDto =
    SykepengegrunnlagResponseDto(
        sykepengegrunnlag = sykepengegrunnlag?.tilSykepengegrunnlagBaseDto(),
        sammenlikningsgrunnlag = sammenlikningsgrunnlag?.tilSammenlikningsgrunnlagDto(),
        opprettetForBehandling = opprettetForBehandling,
    )

private fun SykepengegrunnlagBase.tilSykepengegrunnlagBaseDto() =
    when (this) {
        is Sykepengegrunnlag ->
            SykepengegrunnlagDto(
                grunnbeløp = grunnbeløp.beløp.toDouble(),
                sykepengegrunnlag = sykepengegrunnlag.beløp.toDouble(),
                seksG = seksG.beløp.toDouble(),
                begrensetTil6G = begrensetTil6G,
                grunnbeløpVirkningstidspunkt = grunnbeløpVirkningstidspunkt,
                beregningsgrunnlag = beregningsgrunnlag.beløp.toDouble(),
                næringsdel = næringsdel?.tilNæringsdelDto(),
                kombinertBeregningskode = kombinertBeregningskode?.tilBeregningskoderKombinasjonerSykepengegrunnlagDto(),
            )

        is FrihåndSykepengegrunnlag ->
            FrihåndSykepengegrunnlagDto(
                grunnbeløp = grunnbeløp.beløp.toDouble(),
                sykepengegrunnlag = sykepengegrunnlag.beløp.toDouble(),
                seksG = seksG.beløp.toDouble(),
                begrensetTil6G = begrensetTil6G,
                grunnbeløpVirkningstidspunkt = grunnbeløpVirkningstidspunkt,
                beregningsgrunnlag = beregningsgrunnlag.beløp.toDouble(),
                begrunnelse = begrunnelse,
                beregningskoder = beregningskoder.map { it.tilBeregningskoderSykepengegrunnlagDto() },
            )
    }

private fun Næringsdel.tilNæringsdelDto(): NæringsdelDto =
    NæringsdelDto(
        pensjonsgivendeÅrsinntekt = pensjonsgivendeÅrsinntekt.beløp.toDouble(),
        pensjonsgivendeÅrsinntekt6GBegrenset = pensjonsgivendeÅrsinntekt6GBegrenset.beløp.toDouble(),
        pensjonsgivendeÅrsinntektBegrensetTil6G = pensjonsgivendeÅrsinntektBegrensetTil6G,
        næringsdel = næringsdel.beløp.toDouble(),
        sumAvArbeidsinntekt = sumAvArbeidsinntekt.beløp.toDouble(),
    )

private fun Sammenlikningsgrunnlag.tilSammenlikningsgrunnlagDto(): SammenlikningsgrunnlagDto =
    SammenlikningsgrunnlagDto(
        totaltSammenlikningsgrunnlag = totaltSammenlikningsgrunnlag.beløp.toDouble(),
        avvikProsent = avvikProsent,
        avvikMotInntektsgrunnlag = avvikMotInntektsgrunnlag.beløp.toDouble(),
        basertPåDokumentId = basertPåDokumentId,
    )

private fun BeregningskoderSykepengegrunnlagDto.tilBeregningskoderSykepengegrunnlag(): BeregningskoderSykepengegrunnlag =
    when (this) {
        BeregningskoderSykepengegrunnlagDto.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_HOVEDREGEL -> BeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_HOVEDREGEL
        BeregningskoderSykepengegrunnlagDto.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_SKJOENN_AVVIK -> BeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_SKJOENN_AVVIK
        BeregningskoderSykepengegrunnlagDto.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_SKJOENN_URIKTIG -> BeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_SKJOENN_URIKTIG
        BeregningskoderSykepengegrunnlagDto.FRILANSER_SYKEPENGEGRUNNLAG_HOVEDREGEL -> BeregningskoderSykepengegrunnlag.FRILANSER_SYKEPENGEGRUNNLAG_HOVEDREGEL
        BeregningskoderSykepengegrunnlagDto.FRILANSER_SYKEPENGEGRUNNLAG_SKJOENN_AVVIK -> BeregningskoderSykepengegrunnlag.FRILANSER_SYKEPENGEGRUNNLAG_SKJOENN_AVVIK
        BeregningskoderSykepengegrunnlagDto.FRILANSER_SYKEPENGEGRUNNLAG_SKJOENN_URIKTIG -> BeregningskoderSykepengegrunnlag.FRILANSER_SYKEPENGEGRUNNLAG_SKJOENN_URIKTIG
        BeregningskoderSykepengegrunnlagDto.SELVSTENDIG_SYKEPENGEGRUNNLAG_HOVEDREGEL -> BeregningskoderSykepengegrunnlag.SELVSTENDIG_SYKEPENGEGRUNNLAG_HOVEDREGEL
        BeregningskoderSykepengegrunnlagDto.SELVSTENDIG_SYKEPENGEGRUNNLAG_SKJOENN_VARIG_ENDRING -> BeregningskoderSykepengegrunnlag.SELVSTENDIG_SYKEPENGEGRUNNLAG_SKJOENN_VARIG_ENDRING
        BeregningskoderSykepengegrunnlagDto.SELVSTENDIG_SYKEPENGEGRUNNLAG_SKJOENN_NYIARB -> BeregningskoderSykepengegrunnlag.SELVSTENDIG_SYKEPENGEGRUNNLAG_SKJOENN_NYIARB
        BeregningskoderSykepengegrunnlagDto.INAKTIV_SYKEPENGEGRUNNLAG_HOVEDREGEL -> BeregningskoderSykepengegrunnlag.INAKTIV_SYKEPENGEGRUNNLAG_HOVEDREGEL
        BeregningskoderSykepengegrunnlagDto.INAKTIV_SYKEPENGEGRUNNLAG_SKJOENN_VARIG_ENDRING -> BeregningskoderSykepengegrunnlag.INAKTIV_SYKEPENGEGRUNNLAG_SKJOENN_VARIG_ENDRING
        BeregningskoderSykepengegrunnlagDto.INAKTIV_SYKEPENGEGRUNNLAG_SKJOENN_NYIARB -> BeregningskoderSykepengegrunnlag.INAKTIV_SYKEPENGEGRUNNLAG_SKJOENN_NYIARB
        BeregningskoderSykepengegrunnlagDto.ARBEIDSLEDIG_SYKEPENGEGRUNNLAG_DAGPENGER -> BeregningskoderSykepengegrunnlag.ARBEIDSLEDIG_SYKEPENGEGRUNNLAG_DAGPENGER
        BeregningskoderSykepengegrunnlagDto.ARBEIDSLEDIG_SYKEPENGEGRUNNLAG_VENTELOENN -> BeregningskoderSykepengegrunnlag.ARBEIDSLEDIG_SYKEPENGEGRUNNLAG_VENTELOENN
        BeregningskoderSykepengegrunnlagDto.ARBEIDSLEDIG_SYKEPENGEGRUNNLAG_VARTPENGER -> BeregningskoderSykepengegrunnlag.ARBEIDSLEDIG_SYKEPENGEGRUNNLAG_VARTPENGER
        BeregningskoderSykepengegrunnlagDto.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_TIDSBEGRENSET_FOER_SLUTTDATO -> BeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_TIDSBEGRENSET_FOER_SLUTTDATO
    }

private fun BeregningskoderSykepengegrunnlag.tilBeregningskoderSykepengegrunnlagDto(): BeregningskoderSykepengegrunnlagDto =
    when (this) {
        BeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_HOVEDREGEL -> BeregningskoderSykepengegrunnlagDto.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_HOVEDREGEL
        BeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_SKJOENN_AVVIK -> BeregningskoderSykepengegrunnlagDto.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_SKJOENN_AVVIK
        BeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_SKJOENN_URIKTIG -> BeregningskoderSykepengegrunnlagDto.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_SKJOENN_URIKTIG
        BeregningskoderSykepengegrunnlag.FRILANSER_SYKEPENGEGRUNNLAG_HOVEDREGEL -> BeregningskoderSykepengegrunnlagDto.FRILANSER_SYKEPENGEGRUNNLAG_HOVEDREGEL
        BeregningskoderSykepengegrunnlag.FRILANSER_SYKEPENGEGRUNNLAG_SKJOENN_AVVIK -> BeregningskoderSykepengegrunnlagDto.FRILANSER_SYKEPENGEGRUNNLAG_SKJOENN_AVVIK
        BeregningskoderSykepengegrunnlag.FRILANSER_SYKEPENGEGRUNNLAG_SKJOENN_URIKTIG -> BeregningskoderSykepengegrunnlagDto.FRILANSER_SYKEPENGEGRUNNLAG_SKJOENN_URIKTIG
        BeregningskoderSykepengegrunnlag.SELVSTENDIG_SYKEPENGEGRUNNLAG_HOVEDREGEL -> BeregningskoderSykepengegrunnlagDto.SELVSTENDIG_SYKEPENGEGRUNNLAG_HOVEDREGEL
        BeregningskoderSykepengegrunnlag.SELVSTENDIG_SYKEPENGEGRUNNLAG_SKJOENN_VARIG_ENDRING -> BeregningskoderSykepengegrunnlagDto.SELVSTENDIG_SYKEPENGEGRUNNLAG_SKJOENN_VARIG_ENDRING
        BeregningskoderSykepengegrunnlag.SELVSTENDIG_SYKEPENGEGRUNNLAG_SKJOENN_NYIARB -> BeregningskoderSykepengegrunnlagDto.SELVSTENDIG_SYKEPENGEGRUNNLAG_SKJOENN_NYIARB
        BeregningskoderSykepengegrunnlag.INAKTIV_SYKEPENGEGRUNNLAG_HOVEDREGEL -> BeregningskoderSykepengegrunnlagDto.INAKTIV_SYKEPENGEGRUNNLAG_HOVEDREGEL
        BeregningskoderSykepengegrunnlag.INAKTIV_SYKEPENGEGRUNNLAG_SKJOENN_VARIG_ENDRING -> BeregningskoderSykepengegrunnlagDto.INAKTIV_SYKEPENGEGRUNNLAG_SKJOENN_VARIG_ENDRING
        BeregningskoderSykepengegrunnlag.INAKTIV_SYKEPENGEGRUNNLAG_SKJOENN_NYIARB -> BeregningskoderSykepengegrunnlagDto.INAKTIV_SYKEPENGEGRUNNLAG_SKJOENN_NYIARB
        BeregningskoderSykepengegrunnlag.ARBEIDSLEDIG_SYKEPENGEGRUNNLAG_DAGPENGER -> BeregningskoderSykepengegrunnlagDto.ARBEIDSLEDIG_SYKEPENGEGRUNNLAG_DAGPENGER
        BeregningskoderSykepengegrunnlag.ARBEIDSLEDIG_SYKEPENGEGRUNNLAG_VENTELOENN -> BeregningskoderSykepengegrunnlagDto.ARBEIDSLEDIG_SYKEPENGEGRUNNLAG_VENTELOENN
        BeregningskoderSykepengegrunnlag.ARBEIDSLEDIG_SYKEPENGEGRUNNLAG_VARTPENGER -> BeregningskoderSykepengegrunnlagDto.ARBEIDSLEDIG_SYKEPENGEGRUNNLAG_VARTPENGER
        BeregningskoderSykepengegrunnlag.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_TIDSBEGRENSET_FOER_SLUTTDATO -> BeregningskoderSykepengegrunnlagDto.ARBEIDSTAKER_SYKEPENGEGRUNNLAG_TIDSBEGRENSET_FOER_SLUTTDATO
    }

private fun BeregningskoderKombinasjonerSykepengegrunnlag.tilBeregningskoderKombinasjonerSykepengegrunnlagDto(): BeregningskoderKombinasjonerSykepengegrunnlagDto =
    when (this) {
        BeregningskoderKombinasjonerSykepengegrunnlag.KOMBINERT_ARBEIDSTAKER_SELVSTENDIG_SYKEPENGEGRUNNLAG -> BeregningskoderKombinasjonerSykepengegrunnlagDto.KOMBINERT_ARBEIDSTAKER_SELVSTENDIG_SYKEPENGEGRUNNLAG
        BeregningskoderKombinasjonerSykepengegrunnlag.KOMBINERT_ARBEIDSTAKER_FRILANSER_SYKEPENGEGRUNNLAG -> BeregningskoderKombinasjonerSykepengegrunnlagDto.KOMBINERT_ARBEIDSTAKER_FRILANSER_SYKEPENGEGRUNNLAG
        BeregningskoderKombinasjonerSykepengegrunnlag.KOMBINERT_ARBEIDSTAKER_FRILANSER_SELVSTENDIG_SYKEPENGEGRUNNLAG -> BeregningskoderKombinasjonerSykepengegrunnlagDto.KOMBINERT_ARBEIDSTAKER_FRILANSER_SELVSTENDIG_SYKEPENGEGRUNNLAG
        BeregningskoderKombinasjonerSykepengegrunnlag.KOMBINERT_SELVSTENDIG_FRILANSER_SYKEPENGEGRUNNLAG -> BeregningskoderKombinasjonerSykepengegrunnlagDto.KOMBINERT_SELVSTENDIG_FRILANSER_SYKEPENGEGRUNNLAG
    }
