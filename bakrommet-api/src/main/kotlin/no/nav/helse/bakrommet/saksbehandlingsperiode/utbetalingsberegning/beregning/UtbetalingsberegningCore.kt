package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.beregning

import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.UtbetalingsberegningInput
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.YrkesaktivitetUtbetalingsberegning
import no.nav.helse.bakrommet.saksbehandlingsperiode.yrkesaktivitet.hentDekningsgrad
import no.nav.helse.bakrommet.økonomi.tilInntekt
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt

/**
 * Kjernefunksjoner for utbetalingsberegning - ren funksjonell tilnærming
 *
 * Hovedfunksjon for å beregne utbetalinger for alle yrkesaktiviteter
 */
fun beregnUtbetalingerForAlleYrkesaktiviteter(input: UtbetalingsberegningInput): List<YrkesaktivitetUtbetalingsberegning> {
    val refusjonstidslinjer =
        beregnAlleRefusjonstidslinjer(
            input.yrkesaktivitet,
            input.saksbehandlingsperiode,
        )

    val yrkesaktivitetMedDekningsgrad =
        input.yrkesaktivitet.map { ya ->
            ya to ya.kategorisering.hentDekningsgrad()
        }

    // TODO her

    val utbetalingstidslinjer =
        yrkesaktivitetMedDekningsgrad.map { (yrkesaktivitet, dekningsgrad) ->
            val refusjonstidslinjeData = refusjonstidslinjer[yrkesaktivitet] ?: emptyMap()
            val refusjonstidslinje = opprettRefusjonstidslinjeFraData(refusjonstidslinjeData)
            val fastsattÅrsinntekt = finnInntektForYrkesaktivitet(input.sykepengegrunnlag, yrkesaktivitet)
            val inntektjusteringer = Beløpstidslinje(emptyList()) // TODO: Dette er tilkommen inntekt?

            byggUtbetalingstidslinjeForYrkesaktivitet(
                yrkesaktivitet = yrkesaktivitet,
                dekningsgrad = dekningsgrad.verdi,
                input = input,
                refusjonstidslinje = refusjonstidslinje,
                fastsattÅrsinntekt = fastsattÅrsinntekt ?: Inntekt.INGEN,
            )
        }

    val utbetalingstidslinjerMedTotalGrad = Utbetalingstidslinje.totalSykdomsgrad(utbetalingstidslinjer)
    val sykepengegrunnlag = input.sykepengegrunnlag.sykepengegrunnlag.tilInntekt()
    val utbetalingstidslinjerBetalt =
        Utbetalingstidslinje.betale(
            sykepengegrunnlagBegrenset6G = sykepengegrunnlag,
            tidslinjer = utbetalingstidslinjerMedTotalGrad,
        )

    return yrkesaktivitetMedDekningsgrad
        .zip(
            utbetalingstidslinjerBetalt,
        ).map { (yrkesaktivitetMedDekningsgrad, utbetalingstidslinje) ->
            val (yrkesaktivitet, dekningsgrad) = yrkesaktivitetMedDekningsgrad
            YrkesaktivitetUtbetalingsberegning(
                yrkesaktivitetId = yrkesaktivitet.id,
                utbetalingstidslinje = utbetalingstidslinje,
                dekningsgrad = dekningsgrad,
            )
        }
}
