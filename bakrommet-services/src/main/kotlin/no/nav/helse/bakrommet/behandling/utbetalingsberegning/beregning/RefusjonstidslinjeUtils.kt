package no.nav.helse.bakrommet.behandling.utbetalingsberegning.beregning

import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.Yrkesaktivitetsperiode
import no.nav.helse.dto.PeriodeDto
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate

fun beregnRefusjonstidslinje(
    yrkesaktivitetsperiode: Yrkesaktivitetsperiode,
    saksbehandlingsperiode: PeriodeDto,
): Map<LocalDate, Inntekt> {
    val refusjonstidslinje = mutableMapOf<LocalDate, Inntekt>()

    yrkesaktivitetsperiode.refusjon?.map { refusjon ->
        // Fyll tidslinjen for hver dag i refusjonsperioden
        val refusjonTom = refusjon.tom ?: saksbehandlingsperiode.tom
        refusjon.fom.datesUntil(refusjonTom.plusDays(1)).forEach { dato ->
            refusjonstidslinje[dato] = refusjon.beløp
        }
    }

    return refusjonstidslinje
}

/**
 * Beregner refusjonstidslinjer for alle yrkesaktiviteter
 */
fun beregnAlleRefusjonstidslinjer(
    yrkesaktiviteter: List<Yrkesaktivitetsperiode>,
    saksbehandlingsperiode: PeriodeDto,
): Map<Yrkesaktivitetsperiode, Map<LocalDate, Inntekt>> =
    yrkesaktiviteter.associateWith { yrkesaktivitet ->
        beregnRefusjonstidslinje(yrkesaktivitet, saksbehandlingsperiode)
    }
