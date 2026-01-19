package no.nav.helse.bakrommet.behandling.inntekter.inntektsfastsettelse

import no.nav.helse.bakrommet.domain.sykepenger.BeregningskoderSykepengegrunnlag
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.ArbeidsledigInntektRequest
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.InntektData
import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.InntektRequest

internal fun InntektRequest.Arbeidsledig.arbeidsledigFastsettelse(): InntektData =
    when (val data = data) {
        is ArbeidsledigInntektRequest.Dagpenger -> {
            InntektData.Arbeidsledig(
                omregnetÅrsinntekt = data.dagbeløp,
                sporing = BeregningskoderSykepengegrunnlag.ARBEIDSLEDIG_SYKEPENGEGRUNNLAG_DAGPENGER,
            )
        }

        is ArbeidsledigInntektRequest.Vartpenger -> {
            InntektData.Arbeidsledig(
                omregnetÅrsinntekt = data.årsinntekt,
                sporing = BeregningskoderSykepengegrunnlag.ARBEIDSLEDIG_SYKEPENGEGRUNNLAG_VARTPENGER,
            )
        }

        is ArbeidsledigInntektRequest.Ventelønn -> {
            InntektData.Arbeidsledig(
                omregnetÅrsinntekt = data.årsinntekt,
                sporing = BeregningskoderSykepengegrunnlag.ARBEIDSLEDIG_SYKEPENGEGRUNNLAG_VENTELOENN,
            )
        }
    }
