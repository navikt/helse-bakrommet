package no.nav.helse.bakrommet.behandling.utbetalingsberegning.beregning

import no.nav.helse.bakrommet.behandling.yrkesaktivitet.domene.LegacyYrkesaktivitet
import no.nav.helse.bakrommet.økonomi.tilInntekt
import no.nav.helse.dto.PeriodeDto
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate

fun beregnRefusjonstidslinje(
    legacyYrkesaktivitet: LegacyYrkesaktivitet,
    saksbehandlingsperiode: PeriodeDto,
): Map<LocalDate, Inntekt> {
    val refusjonstidslinje = mutableMapOf<LocalDate, Inntekt>()

    legacyYrkesaktivitet.refusjon?.map { refusjon ->
        // Fyll tidslinjen for hver dag i refusjonsperioden
        val refusjonTom = refusjon.tom ?: saksbehandlingsperiode.tom
        refusjon.fom.datesUntil(refusjonTom.plusDays(1)).forEach { dato ->
            refusjonstidslinje[dato] = refusjon.beløp.tilInntekt().rundTilDaglig()
        }
    }

    return refusjonstidslinje
}

/**
 * Beregner refusjonstidslinjer for alle yrkesaktiviteter
 */
fun beregnAlleRefusjonstidslinjer(
    yrkesaktiviteter: List<LegacyYrkesaktivitet>,
    saksbehandlingsperiode: PeriodeDto,
): Map<LegacyYrkesaktivitet, Map<LocalDate, Inntekt>> =

    yrkesaktiviteter.associateWith { yrkesaktivitet ->
        beregnRefusjonstidslinje(yrkesaktivitet, saksbehandlingsperiode)
    }
