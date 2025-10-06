package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.beregning

import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.UtbetalingsberegningInput
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.YrkesaktivitetUtbetalingsberegning
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.hentDekningsgrad
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje

/**
 * Kjernefunksjoner for utbetalingsberegning - ren funksjonell tilnærming
 *
 * Hovedfunksjon for å beregne utbetalinger for alle yrkesaktiviteter
 */
fun beregnUtbetalingerForAlleYrkesaktiviteter(input: UtbetalingsberegningInput): List<YrkesaktivitetUtbetalingsberegning> {
    val refusjonstidslinjer =
        beregnAlleRefusjonstidslinjer(
            input.sykepengegrunnlag,
            input.yrkesaktivitet.map { it.id },
            input.saksbehandlingsperiode,
        )

    val yrkesaktivitetMedDekningsgrad =
        input.yrkesaktivitet.map { ya ->
            ya to ya.hentDekningsgrad()
        }

    val utbetalingstidslinjer =
        yrkesaktivitetMedDekningsgrad.map { (yrkesaktivitet, dekningsgrad) ->
            val refusjonstidslinjeData = refusjonstidslinjer[yrkesaktivitet.id] ?: emptyMap()
            val refusjonstidslinje = opprettRefusjonstidslinjeFraData(refusjonstidslinjeData)
            val fastsattÅrsinntekt = finnInntektForYrkesaktivitet(input.sykepengegrunnlag, yrkesaktivitet.id)
            val inntektjusteringer = Beløpstidslinje(emptyList()) // TODO: Dette er tilkommen inntekt?

            byggUtbetalingstidslinjeForYrkesaktivitet(
                yrkesaktivitet = yrkesaktivitet,
                dekningsgrad = dekningsgrad.verdi,
                input = input,
                refusjonstidslinje = refusjonstidslinje,
                fastsattÅrsinntekt = fastsattÅrsinntekt,
            )
        }

    val utbetalingstidslinjerMedTotalGrad = Utbetalingstidslinje.totalSykdomsgrad(utbetalingstidslinjer)
    val sykepengegrunnlagBegrenset6G = beregn6GBegrensetSykepengegrunnlag(input.sykepengegrunnlag)
    val utbetalingstidslinjerMed6GBegrensning = Utbetalingstidslinje.betale(sykepengegrunnlagBegrenset6G, utbetalingstidslinjerMedTotalGrad)

    return yrkesaktivitetMedDekningsgrad.zip(
        utbetalingstidslinjerMed6GBegrensning,
    ).map { (yrkesaktivitetMedDekningsgrad, utbetalingstidslinje) ->
        val (yrkesaktivitet, dekningsgrad) = yrkesaktivitetMedDekningsgrad
        YrkesaktivitetUtbetalingsberegning(
            yrkesaktivitetId = yrkesaktivitet.id,
            utbetalingstidslinje = utbetalingstidslinje,
            dekningsgrad = dekningsgrad,
        )
    }
}
