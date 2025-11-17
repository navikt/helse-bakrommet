package no.nav.helse.bakrommet.behandling.inntekter.inntektsfastsettelse

import no.nav.helse.bakrommet.BeregningskoderSykepengegrunnlag
import no.nav.helse.bakrommet.behandling.inntekter.ArbeidsledigInntektRequest
import no.nav.helse.bakrommet.behandling.inntekter.InntektData
import no.nav.helse.bakrommet.behandling.inntekter.InntektRequest
import no.nav.helse.økonomi.Inntekt

internal fun InntektRequest.Arbeidsledig.arbeidsledigFastsettelse(): InntektData =
    when (data) {
        is ArbeidsledigInntektRequest.Dagpenger -> {
            InntektData.Arbeidsledig(
                omregnetÅrsinntekt = Inntekt.gjenopprett(data.dagbeløp).dto().årlig,
                sporing = BeregningskoderSykepengegrunnlag.ARBEIDSLEDIG_SYKEPENGEGRUNNLAG_DAGPENGER,
            )
        }

        is ArbeidsledigInntektRequest.Vartpenger ->
            InntektData.Arbeidsledig(
                omregnetÅrsinntekt = Inntekt.gjenopprett(data.årsinntekt).dto().årlig,
                sporing = BeregningskoderSykepengegrunnlag.ARBEIDSLEDIG_SYKEPENGEGRUNNLAG_VARTPENGER,
            )

        is ArbeidsledigInntektRequest.Ventelønn ->
            InntektData.Arbeidsledig(
                omregnetÅrsinntekt = Inntekt.gjenopprett(data.årsinntekt).dto().årlig,
                sporing = BeregningskoderSykepengegrunnlag.ARBEIDSLEDIG_SYKEPENGEGRUNNLAG_VENTELOENN,
            )
    }
