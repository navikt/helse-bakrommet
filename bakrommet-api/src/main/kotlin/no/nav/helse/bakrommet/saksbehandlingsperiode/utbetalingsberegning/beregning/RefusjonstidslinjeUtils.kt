package no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.beregning

import no.nav.helse.bakrommet.saksbehandlingsperiode.sykepengegrunnlag.SykepengegrunnlagResponse
import no.nav.helse.bakrommet.saksbehandlingsperiode.utbetalingsberegning.UtbetalingsberegningKonfigurasjon
import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.dto.PeriodeDto
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.util.UUID

/**
 * Beregner refusjonstidslinje for en spesifikk yrkesaktivitet
 */
fun beregnRefusjonstidslinje(
    sykepengegrunnlag: SykepengegrunnlagResponse,
    yrkesaktivitetId: UUID,
    saksbehandlingsperiode: PeriodeDto,
): Map<LocalDate, Inntekt> {
    val refusjonstidslinje = mutableMapOf<LocalDate, Inntekt>()

    sykepengegrunnlag.inntekter
        .filter { it.yrkesaktivitetId == yrkesaktivitetId }
        .flatMap { inntekt ->
            inntekt.refusjon.map { refusjon ->
                // Fyll tidslinjen for hver dag i refusjonsperioden
                val refusjonTom = refusjon.tom ?: saksbehandlingsperiode.tom
                refusjon.fom.datesUntil(refusjonTom.plusDays(1)).forEach { dato ->
                    val dagligBeløpKroner = konverterMånedligØreTilDagligKroner(refusjon.beløpØre)
                    val beløp = Inntekt.gjenopprett(InntektbeløpDto.DagligInt(dagligBeløpKroner))
                    refusjonstidslinje[dato] = beløp
                }
            }
        }

    return refusjonstidslinje
}

/**
 * Beregner refusjonstidslinjer for alle yrkesaktiviteter
 */
fun beregnAlleRefusjonstidslinjer(
    sykepengegrunnlag: SykepengegrunnlagResponse,
    yrkesaktivitetIds: List<UUID>,
    saksbehandlingsperiode: PeriodeDto,
): Map<UUID, Map<LocalDate, Inntekt>> =
    yrkesaktivitetIds.associateWith { yrkesaktivitetId ->
        beregnRefusjonstidslinje(sykepengegrunnlag, yrkesaktivitetId, saksbehandlingsperiode)
    }

/**
 * Konverterer månedlig beløp i øre til daglig beløp i kroner
 */
private fun konverterMånedligØreTilDagligKroner(månedligBeløpØre: Long): Int =
    (
        (månedligBeløpØre * UtbetalingsberegningKonfigurasjon.MÅNEDLIG_TIL_ÅRLIG_FAKTOR) /
            UtbetalingsberegningKonfigurasjon.STANDARD_ÅRLIGE_ARBEIDSDAGER /
            UtbetalingsberegningKonfigurasjon.ØRE_TIL_KRONER_FAKTOR
    ).toInt()
