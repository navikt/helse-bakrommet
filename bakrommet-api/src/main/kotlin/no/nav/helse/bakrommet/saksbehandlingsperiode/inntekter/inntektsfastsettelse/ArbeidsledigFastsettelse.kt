package no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.inntektsfastsettelse

import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.ArbeidsledigInntektRequest
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.InntektData
import no.nav.helse.bakrommet.saksbehandlingsperiode.inntekter.InntektRequest
import no.nav.helse.økonomi.Inntekt

internal fun InntektRequest.Arbeidsledig.arbeidsledigFastsettelse(): InntektData =
    when (data) {
        is ArbeidsledigInntektRequest.Dagpenger -> {
            InntektData.Arbeidsledig(
                omregnetÅrsinntekt = Inntekt.gjenopprett(data.dagbeløp).dto().årlig,
            )
        }

        is ArbeidsledigInntektRequest.Vartpenger ->
            InntektData.Arbeidsledig(
                omregnetÅrsinntekt = Inntekt.gjenopprett(data.årsinntekt).dto().årlig,
            )

        is ArbeidsledigInntektRequest.Ventelønn ->
            InntektData.Arbeidsledig(
                omregnetÅrsinntekt = Inntekt.gjenopprett(data.årsinntekt).dto().årlig,
            )
    }
