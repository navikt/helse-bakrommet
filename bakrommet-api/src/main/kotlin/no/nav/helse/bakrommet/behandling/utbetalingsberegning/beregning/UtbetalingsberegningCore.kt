package no.nav.helse.bakrommet.behandling.utbetalingsberegning.beregning

import no.nav.helse.bakrommet.behandling.tilkommen.TilkommenInntektDbRecord
import no.nav.helse.bakrommet.behandling.utbetalingsberegning.Sporbar
import no.nav.helse.bakrommet.behandling.utbetalingsberegning.UtbetalingsberegningInput
import no.nav.helse.bakrommet.behandling.utbetalingsberegning.YrkesaktivitetUtbetalingsberegning
import no.nav.helse.bakrommet.behandling.yrkesaktivitet.hentDekningsgrad
import no.nav.helse.bakrommet.økonomi.tilInntekt
import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.dto.PeriodeDto
import no.nav.helse.dto.ProsentdelDto
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
            input.yrkesaktivitet,
            input.saksbehandlingsperiode,
        )

    val yrkesaktivitetMedDekningsgrad =
        input.yrkesaktivitet.map { ya ->
            ya to ya.kategorisering.hentDekningsgrad()
        }

    val sykepengegrunnlagDaglig = input.sykepengegrunnlag.sykepengegrunnlag.tilInntekt().daglig

    val maksInntektTilFordelingPerDagMap =
        konstruerMaksInntektTilFordelingPerDagMap(
            yrkesaktivitetMedDekningsgrad = yrkesaktivitetMedDekningsgrad,
            refusjonstidslinjer = refusjonstidslinjer,
            sykepengegrunnlag = input.sykepengegrunnlag,
            saksbehandlingsperiode = input.saksbehandlingsperiode,
        )

    val justerteBeløpPerDagOgYrkesaktivitet =
        justerBeløpPerDag(
            maksInntektTilFordelingPerDagMap = maksInntektTilFordelingPerDagMap,
            sykepengegrunnlagDaglig = sykepengegrunnlagDaglig,
            saksbehandlingsperiode = input.saksbehandlingsperiode,
        )

    val justerteMaksInntektTilFordelingPerDagMap =
        byggBeløpstidslinjeFraJusterteBeløp(
            maksInntektTilFordelingPerDagMap = maksInntektTilFordelingPerDagMap,
            justerteBeløpPerDagOgYrkesaktivitet = justerteBeløpPerDagOgYrkesaktivitet,
            saksbehandlingsperiode = input.saksbehandlingsperiode,
        )

    verifiserSumMaksInntektTilFordelingPerDag(
        justerteMaksInntektTilFordelingPerDagMap = justerteMaksInntektTilFordelingPerDagMap,
        maksInntektTilFordelingPerDagMap = maksInntektTilFordelingPerDagMap,
        sykepengegrunnlagDaglig = sykepengegrunnlagDaglig,
        saksbehandlingsperiode = input.saksbehandlingsperiode,
    )

    val utbetalingstidslinjer =
        yrkesaktivitetMedDekningsgrad.map { (yrkesaktivitet, dekningsgrad) ->
            val refusjonstidslinjeData = refusjonstidslinjer[yrkesaktivitet] ?: emptyMap()
            val refusjonstidslinje = opprettRefusjonstidslinjeFraData(refusjonstidslinjeData)
            val inntektjusteringer = input.tilkommenInntekt.tilBeløpstidslinje(input.saksbehandlingsperiode)
            val maksInntektTilFordelingPerDag = justerteMaksInntektTilFordelingPerDagMap[yrkesaktivitet.id]!!

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

/**
 * Konstruerer maksInntektTilFordelingPerDag for alle yrkesaktiviteter
 */
fun konstruerMaksInntektTilFordelingPerDagMap(
    yrkesaktivitetMedDekningsgrad: List<Pair<no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.Yrkesaktivitet, Sporbar<ProsentdelDto>>>,
    refusjonstidslinjer: Map<no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.Yrkesaktivitet, Map<LocalDate, Inntekt>>,
    sykepengegrunnlag: no.nav.helse.bakrommet.behandling.sykepengegrunnlag.SykepengegrunnlagBase,
    saksbehandlingsperiode: PeriodeDto,
): Map<UUID, Beløpstidslinje> {
    return yrkesaktivitetMedDekningsgrad.associate { (yrkesaktivitet, _) ->
        val refusjonstidslinjeData = refusjonstidslinjer[yrkesaktivitet] ?: emptyMap()
        val refusjonstidslinje = opprettRefusjonstidslinjeFraData(refusjonstidslinjeData)

        val beløpsdager =
            saksbehandlingsperiode.fom
                .datesUntil(saksbehandlingsperiode.tom.plusDays(1))
                .map { dato ->
                    val inntektFraSykepengegrunnlag =
                        finnInntektForYrkesaktivitet(
                            sykepengegrunnlag,
                            yrkesaktivitet,
                        )
                    val inntektTilBruk =
                        inntektFraSykepengegrunnlag
                            ?: (refusjonstidslinje[dato] as? Beløpsdag)?.beløp
                            ?: Inntekt.INGEN

                    Beløpsdag(
                        dato = dato,
                        beløp = inntektTilBruk,
                        kilde =
                            Kilde(
                                meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
                                avsender = Avsender.SYKMELDT,
                                tidsstempel = LocalDateTime.now(),
                            ),
                    )
                }.toList()
        yrkesaktivitet.id to Beløpstidslinje(beløpsdager)
    }
}

/**
 * Justerer beløpene dag for dag hvis summen overstiger sykepengegrunnlaget/260
 * Returnerer en map med justerte beløp per dag per yrkesaktivitet
 */
fun justerBeløpPerDag(
    maksInntektTilFordelingPerDagMap: Map<UUID, Beløpstidslinje>,
    sykepengegrunnlagDaglig: Double,
    saksbehandlingsperiode: PeriodeDto,
): Map<Pair<LocalDate, UUID>, Inntekt> {
    val alleDatoer = saksbehandlingsperiode.fom
        .datesUntil(saksbehandlingsperiode.tom.plusDays(1))
        .toList()

    val justerteBeløpPerDagOgYrkesaktivitet = mutableMapOf<Pair<LocalDate, UUID>, Inntekt>()

    alleDatoer.forEach { dato ->
        val totalBeløpForDag = maksInntektTilFordelingPerDagMap.values
            .sumOf { (it[dato] as? Beløpsdag)?.beløp?.daglig ?: 0.0 }

        if (totalBeløpForDag > sykepengegrunnlagDaglig && sykepengegrunnlagDaglig > 0.0) {
            // Juster proporsjonalt
            maksInntektTilFordelingPerDagMap.forEach { (yrkesaktivitetId, tidslinje) ->
                val beløpForDag = (tidslinje[dato] as? Beløpsdag)?.beløp ?: Inntekt.INGEN
                val andel = if (totalBeløpForDag > 0.0) beløpForDag.daglig / totalBeløpForDag else 0.0
                val justertBeløp = InntektbeløpDto.DagligDouble(sykepengegrunnlagDaglig * andel).tilInntekt()
                justerteBeløpPerDagOgYrkesaktivitet[dato to yrkesaktivitetId] = justertBeløp
            }

            // Beregn summen av justerte beløp og juster differansen
            val sumJusterteBeløp = justerteBeløpPerDagOgYrkesaktivitet
                .filterKeys { it.first == dato }
                .values
                .sumOf { it.daglig }
            val differanse = sykepengegrunnlagDaglig - sumJusterteBeløp

            // Legg til differansen på den største yrkesaktiviteten for denne dagen
            if (differanse != 0.0) {
                val størsteYrkesaktivitetId = maksInntektTilFordelingPerDagMap.maxByOrNull { (_, t) ->
                    (t[dato] as? Beløpsdag)?.beløp?.daglig ?: 0.0
                }?.key

                størsteYrkesaktivitetId?.let { id ->
                    val eksisterendeBeløp = justerteBeløpPerDagOgYrkesaktivitet[dato to id] ?: Inntekt.INGEN
                    justerteBeløpPerDagOgYrkesaktivitet[dato to id] =
                        InntektbeløpDto.DagligDouble(eksisterendeBeløp.daglig + differanse).tilInntekt()
                }
            }
        } else {
            // Ingen justering nødvendig
            maksInntektTilFordelingPerDagMap.forEach { (yrkesaktivitetId, tidslinje) ->
                val beløpForDag = (tidslinje[dato] as? Beløpsdag)?.beløp ?: Inntekt.INGEN
                justerteBeløpPerDagOgYrkesaktivitet[dato to yrkesaktivitetId] = beløpForDag
            }
        }
    }

    return justerteBeløpPerDagOgYrkesaktivitet
}

/**
 * Bygger Beløpstidslinje for hver yrkesaktivitet fra justerte beløp
 */
fun byggBeløpstidslinjeFraJusterteBeløp(
    maksInntektTilFordelingPerDagMap: Map<UUID, Beløpstidslinje>,
    justerteBeløpPerDagOgYrkesaktivitet: Map<Pair<LocalDate, UUID>, Inntekt>,
    saksbehandlingsperiode: PeriodeDto,
): Map<UUID, Beløpstidslinje> {
    val alleDatoer = saksbehandlingsperiode.fom
        .datesUntil(saksbehandlingsperiode.tom.plusDays(1))
        .toList()

    return maksInntektTilFordelingPerDagMap.mapValues { (yrkesaktivitetId, tidslinje) ->
        val justerteBeløpsdager = alleDatoer.map { dato ->
            val justertBeløp = justerteBeløpPerDagOgYrkesaktivitet[dato to yrkesaktivitetId]
                ?: ((tidslinje[dato] as? Beløpsdag)?.beløp ?: Inntekt.INGEN)

            Beløpsdag(
                dato = dato,
                beløp = justertBeløp,
                kilde = (tidslinje[dato] as? Beløpsdag)?.kilde
                    ?: Kilde(
                        meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
                        avsender = Avsender.SYKMELDT,
                        tidsstempel = LocalDateTime.now(),
                    ),
            )
        }
        Beløpstidslinje(justerteBeløpsdager)
    }
}

/**
 * Verifiserer at summen av maksInntektTilFordelingPerDag per dag er lik sykepengegrunnlagDaglig
 */
fun verifiserSumMaksInntektTilFordelingPerDag(
    justerteMaksInntektTilFordelingPerDagMap: Map<UUID, Beløpstidslinje>,
    maksInntektTilFordelingPerDagMap: Map<UUID, Beløpstidslinje>,
    sykepengegrunnlagDaglig: Double,
    saksbehandlingsperiode: PeriodeDto,
) {
    val alleDatoer = saksbehandlingsperiode.fom
        .datesUntil(saksbehandlingsperiode.tom.plusDays(1))
        .toList()

    alleDatoer.forEach { dato ->
        val sumMaksInntektTilFordelingPerDag = justerteMaksInntektTilFordelingPerDagMap.values
            .sumOf { (it[dato] as? Beløpsdag)?.beløp?.daglig ?: 0.0 }
        val totalBeløpForDag = maksInntektTilFordelingPerDagMap.values
            .sumOf { (it[dato] as? Beløpsdag)?.beløp?.daglig ?: 0.0 }

        // Hvis totalbeløpet overstiger sykepengegrunnlagDaglig, må summen av justerte beløp være nøyaktig lik sykepengegrunnlagDaglig
        // Hvis ikke, må summen være lik totalbeløpet
        val forventetSum = if (totalBeløpForDag > sykepengegrunnlagDaglig && sykepengegrunnlagDaglig > 0.0) {
            sykepengegrunnlagDaglig
        } else {
            totalBeløpForDag
        }

        check(kotlin.math.abs(sumMaksInntektTilFordelingPerDag - forventetSum) < 0.01) {
            "Verifikasjon feilet for dato $dato: Sum av maksInntektTilFordelingPerDag ($sumMaksInntektTilFordelingPerDag) " +
                "er ikke lik forventet sum ($forventetSum). " +
                "SykepengegrunnlagDaglig: $sykepengegrunnlagDaglig, " +
                "TotalBeløpForDag: $totalBeløpForDag"
        }
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
