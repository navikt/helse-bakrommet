package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.beregning

import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagResponse
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.UtbetalingsberegningFeil
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.UtbetalingsberegningKonfigurasjon
import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.økonomi.Inntekt
import java.util.UUID

/**
 * Finner inntekt for en spesifikk yrkesaktivitet
 */
fun finnInntektForYrkesaktivitet(
    sykepengegrunnlag: SykepengegrunnlagResponse,
    yrkesaktivitetId: UUID,
): Inntekt {
    val inntekt =
        sykepengegrunnlag.inntekter.find { it.yrkesaktivitetId == yrkesaktivitetId }
            ?: throw UtbetalingsberegningFeil.ManglendeInntekt(yrkesaktivitetId)

    return Inntekt.gjenopprett(
        InntektbeløpDto.Årlig(
            inntekt.grunnlagMånedligØre * UtbetalingsberegningKonfigurasjon.MÅNEDLIG_TIL_ÅRLIG_FAKTOR / UtbetalingsberegningKonfigurasjon.ØRE_TIL_KRONER_FAKTOR,
        ),
    )
}

/**
 * Beregner 6G-begrenset sykepengegrunnlag
 */
fun beregn6GBegrensetSykepengegrunnlag(sykepengegrunnlag: SykepengegrunnlagResponse): Inntekt =
    Inntekt.gjenopprett(
        InntektbeløpDto.Årlig(
            beløp =
                minOf(
                    sykepengegrunnlag.sykepengegrunnlagØre,
                    sykepengegrunnlag.grunnbeløp6GØre,
                ) / 100.0,
        ),
    )
