package no.nav.helse.bakrommet.behandling.utbetalingsberegning.beregning

import no.nav.helse.bakrommet.behandling.sykepengegrunnlag.SykepengegrunnlagBase
import no.nav.helse.bakrommet.behandling.tilkommen.TilkommenInntektDbRecord
import no.nav.helse.bakrommet.behandling.utbetalingsberegning.UtbetalingsberegningInput
import no.nav.helse.bakrommet.behandling.utbetalingsberegning.YrkesaktivitetUtbetalingsberegning
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.LegacyYrkesaktivitet
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
import java.time.LocalDate
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
            input.legacyYrkesaktivitet,
            input.saksbehandlingsperiode,
        )

    val yrkesaktivitetMedDekningsgrad =
        input.legacyYrkesaktivitet.map { ya ->
            ya to ya.kategorisering.hentDekningsgrad(input.vilkår)
        }

    val allerDagersMaksInntekter =
        input.saksbehandlingsperiode.fom
            .datesUntil(input.saksbehandlingsperiode.tom.plusDays(1))
            .map { it to input.legacyYrkesaktivitet }
            .toList()
            .associate { it.first to it.second }
            .berikMedAlleYrkesaktivitetersMaksInntektPerDag(
                sykepengenngrunnlag = input.sykepengegrunnlag,
                refusjonstidslinjer = refusjonstidslinjer,
            ).justerOppForLaveDager(sykepengegrunnlag = input.sykepengegrunnlag)

    val utbetalingstidslinjer =
        yrkesaktivitetMedDekningsgrad.map { (yrkesaktivitet, dekningsgrad) ->
            val refusjonstidslinjeData = refusjonstidslinjer[yrkesaktivitet] ?: emptyMap()
            val refusjonstidslinje = opprettRefusjonstidslinjeFraData(refusjonstidslinjeData)
            val inntektjusteringer = input.tilkommenInntekt.tilBeløpstidslinje(input.saksbehandlingsperiode)
            val maksInntektTilFordelingPerDag =
                allerDagersMaksInntekter.skapBeløpstidslinjeForYrkesaktivitet(yrkesaktivitet)

            byggUtbetalingstidslinjeForYrkesaktivitet(
                legacyYrkesaktivitet = yrkesaktivitet,
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

private fun Map<LocalDate, List<Pair<LegacyYrkesaktivitet, Inntekt>>>.skapBeløpstidslinjeForYrkesaktivitet(
    legacyYrkesaktivitet: LegacyYrkesaktivitet,
): Beløpstidslinje {
    val beløpsdager =
        this.mapNotNull { (dato, inntekter) ->
            val inntektForYrkesaktivitet = inntekter.firstOrNull { it.first == legacyYrkesaktivitet }?.second
            if (inntektForYrkesaktivitet != null) {
                Beløpsdag(
                    dato = dato,
                    beløp = inntektForYrkesaktivitet,
                    kilde =
                        Kilde(
                            meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
                            avsender = Avsender.SYKMELDT,
                            tidsstempel = LocalDateTime.now(),
                        ),
                )
            } else {
                null
            }
        }
    return Beløpstidslinje(beløpsdager)
}

private fun Map<LocalDate, List<Pair<LegacyYrkesaktivitet, Inntekt>>>.justerOppForLaveDager(sykepengegrunnlag: SykepengegrunnlagBase): Map<LocalDate, List<Pair<LegacyYrkesaktivitet, Inntekt>>> {
    return this
        .map {
            // Vi finner summen av dagens inntekter for alle yrkesaktiviteter. Hvis summen er lavere enn sykepengegrunnlaget så justerer vi det opp forholdsmessig per yrkesaktivitet gitt deres andel av inntekt
            val dagensSum = it.value.sumOf { (_, inntekt) -> inntekt.daglig }
            if (dagensSum >= sykepengegrunnlag.sykepengegrunnlag.tilInntekt().daglig) {
                return@map it.key to it.value
            } else if (dagensSum == 0.0) {
                // TODO vi må bare fordele sykepengegrunnlaget likt på alle yrkesaktiviteter ? Blir dette riktig?
                val antallYrkesaktiviteter = it.value.size
                val liktFordeltInntektPerYa = sykepengegrunnlag.sykepengegrunnlag.tilInntekt().daglig / antallYrkesaktiviteter
                val inntekterForDato =
                    it.value.map { (yrkesaktivitet, _) ->
                        yrkesaktivitet to InntektbeløpDto.DagligDouble(liktFordeltInntektPerYa).tilInntekt()
                    }

                return@map it.key to inntekterForDato
            } else {
                // Her må vi justere opp.
                val justeringsfaktor = sykepengegrunnlag.sykepengegrunnlag.tilInntekt().daglig / dagensSum
                val justerteInntekterForDato =
                    it.value.map { (yrkesaktivitet, inntekt) ->
                        val justertInntekt =
                            InntektbeløpDto.DagligDouble(inntekt.daglig * justeringsfaktor).tilInntekt()
                        yrkesaktivitet to justertInntekt
                    }
                return@map it.key to justerteInntekterForDato
            }
        }.toMap()
}

private fun Map<LocalDate, List<LegacyYrkesaktivitet>>.berikMedAlleYrkesaktivitetersMaksInntektPerDag(
    sykepengenngrunnlag: SykepengegrunnlagBase,
    refusjonstidslinjer: Map<LegacyYrkesaktivitet, Map<LocalDate, Inntekt>>,
): Map<LocalDate, List<Pair<LegacyYrkesaktivitet, Inntekt>>> =
    this
        .map {
            val inntekterForAlleYrkesaktiviteter =
                it.value.map { ya ->
                    val refusjonstidslinjeForYa = refusjonstidslinjer[ya] ?: emptyMap()
                    // Primært hent inntekt fra sykepengegrunnlag, sekundært fra refusjonstidslinje, tertiært ingen inntekt
                    val inntektForDato = finnInntektForYrkesaktivitet(sykepengenngrunnlag, ya) ?: refusjonstidslinjeForYa[it.key] ?: Inntekt.INGEN
                    ya to inntektForDato
                }
            it.key to inntekterForAlleYrkesaktiviteter
        }.toMap()

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
