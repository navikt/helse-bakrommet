package no.nav.helse.bakrommet.behandling.utbetalingsberegning.beregning

import no.nav.helse.bakrommet.domain.sykepenger.yrkesaktivitet.Yrkesaktivitet
import no.nav.helse.dto.PeriodeDto
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate

fun beregnRefusjonstidslinje(
    yrkesaktivitet: Yrkesaktivitet,
    saksbehandlingsperiode: PeriodeDto,
): Map<LocalDate, Inntekt> {
    val refusjonstidslinje = mutableMapOf<LocalDate, Inntekt>()

    yrkesaktivitet.refusjon?.map { refusjon ->
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
    yrkesaktiviteter: List<Yrkesaktivitet>,
    saksbehandlingsperiode: PeriodeDto,
): Map<Yrkesaktivitet, Map<LocalDate, Inntekt>> =
    yrkesaktiviteter.associateWith { yrkesaktivitet ->
        beregnRefusjonstidslinje(yrkesaktivitet, saksbehandlingsperiode)
    }
