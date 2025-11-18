package no.nav.helse.bakrommet.behandling.utbetalingsberegning.beregning

import no.nav.helse.bakrommet.behandling.tilkommen.TilkommenInntektDbRecord
import no.nav.helse.bakrommet.behandling.utbetalingsberegning.UtbetalingsberegningInput
import no.nav.helse.bakrommet.behandling.utbetalingsberegning.YrkesaktivitetUtbetalingsberegning
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.hentDekningsgrad
import no.nav.helse.bakrommet.økonomi.tilInntekt
import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.dto.PeriodeDto
import no.nav.helse.erHelg
import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.person.beløp.Beløpsdag
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.Kilde
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDateTime
import java.util.UUID

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

    // TODO her tror jeg vi skal hente den virituelle inntekten

    val utbetalingstidslinjer =
        yrkesaktivitetMedDekningsgrad.map { (yrkesaktivitet, dekningsgrad) ->
            val refusjonstidslinjeData = refusjonstidslinjer[yrkesaktivitet] ?: emptyMap()
            val refusjonstidslinje = opprettRefusjonstidslinjeFraData(refusjonstidslinjeData)
            val fastsattÅrsinntekt = finnInntektForYrkesaktivitet(input.sykepengegrunnlag, yrkesaktivitet)
            val inntektjusteringer = input.tilkommenInntekt.tilBeløpstidslinje(input.saksbehandlingsperiode)

            byggUtbetalingstidslinjeForYrkesaktivitet(
                yrkesaktivitet = yrkesaktivitet,
                dekningsgrad = dekningsgrad.verdi,
                input = input,
                refusjonstidslinje = refusjonstidslinje,
                fastsattÅrsinntekt = fastsattÅrsinntekt ?: Inntekt.INGEN,
                inntektjusteringer = inntektjusteringer,
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

fun InntektbeløpDto.DagligDouble.tilInntekt(): Inntekt = Inntekt.gjenopprett(this)

private fun List<TilkommenInntektDbRecord>.tilBeløpstidslinje(periode: PeriodeDto): Beløpstidslinje {
    // lag map av alle datoer i behandlingen til beløp
    val alleDager = periode.fom.datesUntil(periode.tom.plusDays(1)).toList()

    val mapMedTommeInntekter = alleDager.associateWith { Inntekt.INGEN }.toMutableMap()

    this.map { it.tilkommenInntekt }.forEach { t ->
        val dagerIPerioden =
            t.fom
                .datesUntil(
                    t.tom.plusDays(
                        1,
                    ),
                ).filter { !it.erHelg() }
                .filter { it !in t.ekskluderteDager }
                .toList()
                .toMutableSet()
        val beløpPerDag = t.inntektForPerioden / dagerIPerioden.size.toBigDecimal()
        dagerIPerioden.forEach { dag ->
            mapMedTommeInntekter[dag] = mapMedTommeInntekter[dag]!! + InntektbeløpDto.DagligDouble(beløpPerDag.toDouble()).tilInntekt()
        }
    }

    return Beløpstidslinje(
        mapMedTommeInntekter.map { (dato, inntekt) ->
            Beløpsdag(
                dato = dato,
                beløp = inntekt,
                kilde =
                    Kilde(
                        meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
                        avsender = Avsender.SYKMELDT,
                        tidsstempel = LocalDateTime.now(),
                    ),
            )
        },
    )
}
