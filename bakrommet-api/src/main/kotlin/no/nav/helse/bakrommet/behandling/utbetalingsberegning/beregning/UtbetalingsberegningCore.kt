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
import kotlin.collections.set

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

    val sykepengennlag = input.sykepengegrunnlag.sykepengegrunnlag.beløp

    val inntekterForYrkesaktiviteter =
        input.yrkesaktivitet.map {
            it.id to (
                finnManuellInntektForYrkesaktivitet(it) ?: finnInntektForYrkesaktivitet(
                    input.sykepengegrunnlag,
                    it,
                ) ?: Inntekt.INGEN
            )
        }

    val sumAvAlleInntekter = inntekterForYrkesaktiviteter.sumOf { it.second.årlig }

    val andelTilFordelingForYrkesaktiviteter =
        if (input.yrkesaktivitet.size == 1) {
            mapOf(input.yrkesaktivitet.first().id to InntektbeløpDto.Årlig(sykepengennlag).tilInntekt())
        } else {
            inntekterForYrkesaktiviteter.associate { inntekt ->
                val andel = if (sumAvAlleInntekter == 0.0) 0.0 else inntekt.second.årlig / sumAvAlleInntekter
                inntekt.first to InntektbeløpDto.Årlig((sykepengennlag * andel)).tilInntekt()
            }
        }

    val utbetalingstidslinjer =
        yrkesaktivitetMedDekningsgrad.map { (yrkesaktivitet, dekningsgrad) ->
            val refusjonstidslinjeData = refusjonstidslinjer[yrkesaktivitet] ?: emptyMap()
            val refusjonstidslinje = opprettRefusjonstidslinjeFraData(refusjonstidslinjeData)
            val inntektjusteringer = input.tilkommenInntekt.tilBeløpstidslinje(input.saksbehandlingsperiode)
            val andel = andelTilFordelingForYrkesaktiviteter[yrkesaktivitet.id]!!

            val beløpsdager =
                input.saksbehandlingsperiode.fom
                    .datesUntil(input.saksbehandlingsperiode.tom.plusDays(1))
                    .map { dato ->
                        Beløpsdag(
                            dato,
                            finnInntektForYrkesaktivitet(
                                input.sykepengegrunnlag,
                                yrkesaktivitet,
                            ) ?: Inntekt.INGEN,
                            Kilde(
                                meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
                                avsender = Avsender.SYKMELDT,
                                tidsstempel = LocalDateTime.now(),
                            ),
                        )
                    }.toList()
            val maksInntektTilFordelingPerDag = Beløpstidslinje(beløpsdager)

            byggUtbetalingstidslinjeForYrkesaktivitet(
                yrkesaktivitet = yrkesaktivitet,
                dekningsgrad = dekningsgrad.verdi,
                input = input,
                refusjonstidslinje = refusjonstidslinje,
                maksInntektTilFordelingPerDag = maksInntektTilFordelingPerDag,
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
            mapMedTommeInntekter[dag] =
                mapMedTommeInntekter[dag]!! + InntektbeløpDto.DagligDouble(beløpPerDag.toDouble()).tilInntekt()
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
